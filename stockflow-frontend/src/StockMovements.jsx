
import { useEffect, useMemo, useState } from "react";
import { apiFetch as fetch } from "./api/api";

const PAGE_SIZE = 10;

const initialForm = {
    productId: "",
    warehouseId: "",
    sourceWarehouseId: "",
    destinationWarehouseId: "",
    quantity: "",
    reference: "",
    remarks: "",
};

function StockMovements() {
    const [movements, setMovements] = useState([]);
    const [products, setProducts] = useState([]);
    const [warehouses, setWarehouses] = useState([]);

    const [productFilter, setProductFilter] = useState("");
    const [warehouseFilter, setWarehouseFilter] = useState("");
    const [typeFilter, setTypeFilter] = useState("");

    const [currentPage, setCurrentPage] = useState(0);
    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);

    const [errorMessage, setErrorMessage] = useState("");
    const [successMessage, setSuccessMessage] = useState("");

    const [activeOperation, setActiveOperation] = useState("STOCK_IN");
    const [form, setForm] = useState(initialForm);

    useEffect(() => {
        loadPageData();
    }, []);

    const fetchAllMovements = async () => {
        const allMovements = [];
        let page = 0;
        let totalPages = 1;

        while (page < totalPages) {
            const response = await fetch(
                `/api/v1/stock-movements?page=${page}&size=100&sort=createdAt,desc`
            );

            if (!response.ok) {
                const error = await response.json().catch(() => null);
                throw new Error(
                    error?.message || "Unable to fetch stock movements"
                );
            }

            const data = await response.json();

            if (Array.isArray(data.content)) {
                allMovements.push(...data.content);
                totalPages = Number(data.totalPages) || 0;
                page += 1;

                if (data.last === true) {
                    break;
                }
            } else if (Array.isArray(data)) {
                allMovements.push(...data);
                break;
            } else {
                throw new Error(
                    "Unexpected stock movement response format"
                );
            }
        }

        return allMovements;
    };

    const loadPageData = async () => {
        setLoading(true);
        setErrorMessage("");

        try {
            const [movementData, productResponse, warehouseResponse] =
                await Promise.all([
                    fetchAllMovements(),
                    fetch("/api/v1/products"),
                    fetch("/api/v1/warehouses"),
                ]);

            if (!productResponse.ok) {
                throw new Error("Unable to fetch products");
            }

            if (!warehouseResponse.ok) {
                throw new Error("Unable to fetch warehouses");
            }

            const productData = await productResponse.json();
            const warehouseData = await warehouseResponse.json();

            const productList = Array.isArray(productData)
                ? productData
                : productData.content || [];

            const warehouseList = Array.isArray(warehouseData)
                ? warehouseData
                : warehouseData.content || [];

            setMovements(movementData);
            setProducts(productList);

            // Exclude explicitly inactive warehouses from new transactions.
            setWarehouses(
                warehouseList.filter(
                    (warehouse) =>
                        warehouse.active !== false &&
                        warehouse.isActive !== false
                )
            );
        } catch (error) {
            setErrorMessage(error.message);
        } finally {
            setLoading(false);
        }
    };

    const filteredMovements = useMemo(() => {
        return movements.filter((movement) => {
            const matchesProduct =
                !productFilter ||
                String(movement.productId) === productFilter;

            const matchesWarehouse =
                !warehouseFilter ||
                String(movement.warehouseId) === warehouseFilter;

            const matchesType =
                !typeFilter || movement.movementType === typeFilter;

            return matchesProduct && matchesWarehouse && matchesType;
        });
    }, [movements, productFilter, warehouseFilter, typeFilter]);

    useEffect(() => {
        setCurrentPage(0);
    }, [productFilter, warehouseFilter, typeFilter]);

    const totalPages = Math.ceil(filteredMovements.length / PAGE_SIZE);

    const visibleMovements = filteredMovements.slice(
        currentPage * PAGE_SIZE,
        (currentPage + 1) * PAGE_SIZE
    );

    const handleFormChange = (event) => {
        const { name, value } = event.target;

        setForm((previous) => ({
            ...previous,
            [name]: value,
        }));
    };

    const handleOperationChange = (operation) => {
        setActiveOperation(operation);
        setForm(initialForm);
        setErrorMessage("");
        setSuccessMessage("");
    };

    const handleTransactionSubmit = async (event) => {
        event.preventDefault();

        setErrorMessage("");
        setSuccessMessage("");

        if (!form.productId) {
            setErrorMessage("Please select a product.");
            return;
        }

        if (!form.quantity || Number(form.quantity) <= 0) {
            setErrorMessage("Quantity must be greater than zero.");
            return;
        }

        let endpoint;
        let requestBody;

        const commonFields = {
            productId: Number(form.productId),
            quantity: Number(form.quantity),
            reference: form.reference.trim(),
            remarks: form.remarks.trim(),
        };

        if (activeOperation === "STOCK_IN") {
            if (!form.warehouseId) {
                setErrorMessage("Please select a warehouse.");
                return;
            }

            endpoint = "/api/v1/stock-movements/stock-in";

            requestBody = {
                ...commonFields,
                warehouseId: Number(form.warehouseId),
            };
        } else if (activeOperation === "STOCK_OUT") {
            if (!form.warehouseId) {
                setErrorMessage("Please select a warehouse.");
                return;
            }

            endpoint = "/api/v1/stock-movements/stock-out";

            requestBody = {
                ...commonFields,
                warehouseId: Number(form.warehouseId),
            };
        } else {
            if (!form.sourceWarehouseId || !form.destinationWarehouseId) {
                setErrorMessage("Please select both warehouses.");
                return;
            }

            if (
                form.sourceWarehouseId === form.destinationWarehouseId
            ) {
                setErrorMessage(
                    "Source and destination warehouses must be different."
                );
                return;
            }

            endpoint = "/api/v1/stock-movements/transfer";

            requestBody = {
                ...commonFields,
                sourceWarehouseId: Number(form.sourceWarehouseId),
                destinationWarehouseId: Number(
                    form.destinationWarehouseId
                ),
            };
        }

        setSubmitting(true);

        try {
            const response = await fetch(endpoint, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify(requestBody),
            });

            const responseData = await response.json().catch(() => null);

            if (!response.ok) {
                throw new Error(
                    responseData?.message ||
                    responseData?.error ||
                    "Unable to complete stock transaction."
                );
            }

            const successText =
                activeOperation === "STOCK_IN"
                    ? "Stock added successfully."
                    : activeOperation === "STOCK_OUT"
                    ? "Stock issued successfully."
                    : "Stock transferred successfully.";

            setSuccessMessage(successText);
            setForm(initialForm);

            // Reload movement history and available products/warehouses.
            await loadPageData();
        } catch (error) {
            setErrorMessage(error.message);
        } finally {
            setSubmitting(false);
        }
    };

    const clearFilters = () => {
        setProductFilter("");
        setWarehouseFilter("");
        setTypeFilter("");
    };

    const formatDate = (value) => {
        if (!value) {
            return "—";
        }

        const date = new Date(value);

        return Number.isNaN(date.getTime())
            ? value
            : date.toLocaleString();
    };

    const formatQuantity = (value) => {
        const quantity = Number(value);

        return Number.isFinite(quantity)
            ? quantity.toLocaleString()
            : value;
    };

    const operationTitle =
        activeOperation === "STOCK_IN"
            ? "Stock In"
            : activeOperation === "STOCK_OUT"
            ? "Stock Out"
            : "Stock Transfer";

    return (
        <div className="products-container">
            <div className="products-header">
                <h2>Stock Movement Management</h2>
                <p>
                    Add stock, issue stock, transfer between warehouses,
                    and view movement history.
                </p>
            </div>

            {errorMessage && (
                <div className="error-message">
                    {errorMessage}
                </div>
            )}

            {successMessage && (
                <div className="success-message">
                    {successMessage}
                </div>
            )}

            {/* Transaction operation selector */}
            <div className="product-form-container">
                <h3>Stock Operations</h3>

                <div className="form-actions">
                    <button
                        type="button"
                        className={
                            activeOperation === "STOCK_IN"
                                ? "primary-btn"
                                : "cancel-btn"
                        }
                        onClick={() =>
                            handleOperationChange("STOCK_IN")
                        }
                    >
                        Stock In
                    </button>

                    <button
                        type="button"
                        className={
                            activeOperation === "STOCK_OUT"
                                ? "primary-btn"
                                : "cancel-btn"
                        }
                        onClick={() =>
                            handleOperationChange("STOCK_OUT")
                        }
                    >
                        Stock Out
                    </button>

                    <button
                        type="button"
                        className={
                            activeOperation === "TRANSFER"
                                ? "primary-btn"
                                : "cancel-btn"
                        }
                        onClick={() =>
                            handleOperationChange("TRANSFER")
                        }
                    >
                        Stock Transfer
                    </button>
                </div>

                <h3>{operationTitle}</h3>

                <form
                    className="product-form"
                    onSubmit={handleTransactionSubmit}
                >
                    <div className="form-group">
                        <label htmlFor="transaction-product">
                            Product
                        </label>

                        <select
                            id="transaction-product"
                            name="productId"
                            value={form.productId}
                            onChange={handleFormChange}
                            required
                        >
                            <option value="">Select product</option>

                            {products.map((product) => (
                                <option
                                    key={product.id}
                                    value={product.id}
                                >
                                    {product.name} ({product.sku})
                                </option>
                            ))}
                        </select>
                    </div>

                    {activeOperation !== "TRANSFER" ? (
                        <div className="form-group">
                            <label htmlFor="transaction-warehouse">
                                Warehouse
                            </label>

                            <select
                                id="transaction-warehouse"
                                name="warehouseId"
                                value={form.warehouseId}
                                onChange={handleFormChange}
                                required
                            >
                                <option value="">
                                    Select warehouse
                                </option>

                                {warehouses.map((warehouse) => (
                                    <option
                                        key={warehouse.id}
                                        value={warehouse.id}
                                    >
                                        {warehouse.name}
                                    </option>
                                ))}
                            </select>
                        </div>
                    ) : (
                        <>
                            <div className="form-group">
                                <label htmlFor="source-warehouse">
                                    Source Warehouse
                                </label>

                                <select
                                    id="source-warehouse"
                                    name="sourceWarehouseId"
                                    value={form.sourceWarehouseId}
                                    onChange={handleFormChange}
                                    required
                                >
                                    <option value="">
                                        Select source warehouse
                                    </option>

                                    {warehouses.map((warehouse) => (
                                        <option
                                            key={warehouse.id}
                                            value={warehouse.id}
                                            disabled={
                                                String(warehouse.id) ===
                                                form.destinationWarehouseId
                                            }
                                        >
                                            {warehouse.name}
                                        </option>
                                    ))}
                                </select>
                            </div>

                            <div className="form-group">
                                <label htmlFor="destination-warehouse">
                                    Destination Warehouse
                                </label>

                                <select
                                    id="destination-warehouse"
                                    name="destinationWarehouseId"
                                    value={form.destinationWarehouseId}
                                    onChange={handleFormChange}
                                    required
                                >
                                    <option value="">
                                        Select destination warehouse
                                    </option>

                                    {warehouses.map((warehouse) => (
                                        <option
                                            key={warehouse.id}
                                            value={warehouse.id}
                                            disabled={
                                                String(warehouse.id) ===
                                                form.sourceWarehouseId
                                            }
                                        >
                                            {warehouse.name}
                                        </option>
                                    ))}
                                </select>
                            </div>
                        </>
                    )}

                    <div className="form-group">
                        <label htmlFor="transaction-quantity">
                            Quantity
                        </label>

                        <input
                            id="transaction-quantity"
                            type="number"
                            name="quantity"
                            value={form.quantity}
                            onChange={handleFormChange}
                            min="0.001"
                            step="0.001"
                            placeholder="Enter quantity"
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label htmlFor="transaction-reference">
                            Reference (Optional)
                        </label>

                        <input
                            id="transaction-reference"
                            type="text"
                            name="reference"
                            value={form.reference}
                            onChange={handleFormChange}
                            maxLength="100"
                            placeholder="Enter reference number"
                        />
                    </div>

                    <div className="form-group">
                        <label htmlFor="transaction-remarks">
                            Remarks (Optional)
                        </label>

                        <textarea
                            id="transaction-remarks"
                            name="remarks"
                            value={form.remarks}
                            onChange={handleFormChange}
                            maxLength="255"
                            placeholder="Enter remarks"
                            rows="3"
                        />
                    </div>

                    <div className="form-actions">
                        <button
                            type="submit"
                            className="primary-btn"
                            disabled={
                                submitting ||
                                loading ||
                                products.length === 0 ||
                                warehouses.length === 0
                            }
                        >
                            {submitting
                                ? "Processing..."
                                : `Submit ${operationTitle}`}
                        </button>

                        <button
                            type="button"
                            className="cancel-btn"
                            disabled={submitting}
                            onClick={() => setForm(initialForm)}
                        >
                            Clear
                        </button>
                    </div>
                </form>
            </div>

            {/* Movement filters */}
            <div className="product-form-container">
                <h3>Filter Movements</h3>

                <div className="product-form">
                    <div className="form-group">
                        <label htmlFor="movement-product-filter">
                            Product
                        </label>

                        <select
                            id="movement-product-filter"
                            value={productFilter}
                            onChange={(event) =>
                                setProductFilter(event.target.value)
                            }
                        >
                            <option value="">All products</option>

                            {products.map((product) => (
                                <option
                                    key={product.id}
                                    value={product.id}
                                >
                                    {product.name} ({product.sku})
                                </option>
                            ))}
                        </select>
                    </div>

                    <div className="form-group">
                        <label htmlFor="movement-warehouse-filter">
                            Warehouse
                        </label>

                        <select
                            id="movement-warehouse-filter"
                            value={warehouseFilter}
                            onChange={(event) =>
                                setWarehouseFilter(event.target.value)
                            }
                        >
                            <option value="">All warehouses</option>

                            {warehouses.map((warehouse) => (
                                <option
                                    key={warehouse.id}
                                    value={warehouse.id}
                                >
                                    {warehouse.name}
                                </option>
                            ))}
                        </select>
                    </div>

                    <div className="form-group">
                        <label htmlFor="movement-type-filter">
                            Movement Type
                        </label>

                        <select
                            id="movement-type-filter"
                            value={typeFilter}
                            onChange={(event) =>
                                setTypeFilter(event.target.value)
                            }
                        >
                            <option value="">All movement types</option>
                            <option value="STOCK_IN">Stock In</option>
                            <option value="STOCK_OUT">Stock Out</option>
                            <option value="TRANSFER_IN">Transfer In</option>
                            <option value="TRANSFER_OUT">Transfer Out</option>
                            <option value="ADJUSTMENT">Adjustment</option>
                        </select>
                    </div>

                    <div className="form-actions">
                        <button
                            type="button"
                            className="cancel-btn"
                            onClick={clearFilters}
                        >
                            Clear Filters
                        </button>

                        <button
                            type="button"
                            className="primary-btn"
                            onClick={loadPageData}
                            disabled={loading}
                        >
                            {loading ? "Refreshing..." : "Refresh"}
                        </button>
                    </div>
                </div>
            </div>

            {/* Movement history */}
            <div className="products-table-container">
                <div className="products-header">
                    <h3>Movement Records</h3>
                    <p>
                        {filteredMovements.length} record(s) found
                    </p>
                </div>

                {loading ? (
                    <p>Loading stock movements...</p>
                ) : (
                    <>
                        <div className="table-responsive">
                            <table className="products-table">
                                <thead>
                                    <tr>
                                        <th>ID</th>
                                        <th>Date &amp; Time</th>
                                        <th>Product</th>
                                        <th>SKU</th>
                                        <th>Warehouse</th>
                                        <th>Movement Type</th>
                                        <th>Quantity</th>
                                        <th>Reference</th>
                                        <th>Remarks</th>
                                    </tr>
                                </thead>

                                <tbody>
                                    {visibleMovements.length === 0 ? (
                                        <tr>
                                            <td colSpan="9">
                                                No stock movement records found.
                                            </td>
                                        </tr>
                                    ) : (
                                        visibleMovements.map((movement) => (
                                            <tr key={movement.id}>
                                                <td>{movement.id}</td>
                                                <td>
                                                    {formatDate(
                                                        movement.createdAt
                                                    )}
                                                </td>
                                                <td>{movement.productName}</td>
                                                <td>{movement.productSku}</td>
                                                <td>{movement.warehouseName}</td>
                                                <td>
                                                    {movement.movementType?.replaceAll(
                                                        "_",
                                                        " "
                                                    )}
                                                </td>
                                                <td>
                                                    {formatQuantity(
                                                        movement.quantity
                                                    )}
                                                </td>
                                                <td>
                                                    {movement.reference || "—"}
                                                </td>
                                                <td>
                                                    {movement.remarks || "—"}
                                                </td>
                                            </tr>
                                        ))
                                    )}
                                </tbody>
                            </table>
                        </div>

                        {totalPages > 1 && (
                            <div className="form-actions">
                                <button
                                    type="button"
                                    className="cancel-btn"
                                    onClick={() =>
                                        setCurrentPage((page) =>
                                            Math.max(0, page - 1)
                                        )
                                    }
                                    disabled={currentPage === 0}
                                >
                                    Previous
                                </button>

                                <span>
                                    Page {currentPage + 1} of {totalPages}
                                </span>

                                <button
                                    type="button"
                                    className="primary-btn"
                                    onClick={() =>
                                        setCurrentPage((page) =>
                                            Math.min(
                                                totalPages - 1,
                                                page + 1
                                            )
                                        )
                                    }
                                    disabled={
                                        currentPage >= totalPages - 1
                                    }
                                >
                                    Next
                                </button>
                            </div>
                        )}
                    </>
                )}
            </div>
        </div>
    );
}

export default StockMovements;
