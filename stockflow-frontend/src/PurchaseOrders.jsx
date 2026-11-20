
import { useEffect, useState } from "react";
import { apiFetch as fetch } from "./api/api";

const API = "/api/v1";

const emptyItem = () => ({
    productId: "",
    quantity: 1,
    unitPrice: ""
});

function PurchaseOrders() {
    const [orders, setOrders] = useState([]);
    const [suppliers, setSuppliers] = useState([]);
    const [warehouses, setWarehouses] = useState([]);
    const [products, setProducts] = useState([]);

    const [formData, setFormData] = useState({
        supplierId: "",
        warehouseId: "",
        expectedDeliveryDate: "",
        notes: "",
        items: [emptyItem()]
    });

    const [selectedOrder, setSelectedOrder] = useState(null);
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [successMessage, setSuccessMessage] = useState("");
    const [errorMessage, setErrorMessage] = useState("");

    const isSupplierActive = (supplier) =>
        supplier.isActive ?? supplier.active ?? supplier.is_active;

    const activeSuppliers = suppliers.filter(
        (supplier) => isSupplierActive(supplier) === true
    );

    const activeWarehouses = warehouses.filter(
        (warehouse) => warehouse.active === true
    );

    useEffect(() => {
        loadInitialData();
    }, []);

    const readError = async (response, fallback) => {
        const error = await response.json().catch(() => null);
        return error?.message || error?.error || fallback;
    };

    const loadInitialData = async () => {
        setLoading(true);
        setErrorMessage("");

        try {
            const [ordersResponse, suppliersResponse, warehousesResponse, productsResponse] =
                await Promise.all([
                    fetch(`${API}/purchase-orders`),
                    fetch(`${API}/suppliers`),
                    fetch(`${API}/warehouses`),
                    fetch(`${API}/products`)
                ]);

            const responses = [
                ordersResponse,
                suppliersResponse,
                warehousesResponse,
                productsResponse
            ];

            if (responses.some((response) => !response.ok)) {
                throw new Error("Unable to load Purchase Orders data.");
            }

            const [ordersData, suppliersData, warehousesData, productsData] =
                await Promise.all(responses.map((response) => response.json()));

            setOrders(
                Array.isArray(ordersData)
                    ? ordersData
                    : ordersData.content || []
            );
            setSuppliers(Array.isArray(suppliersData) ? suppliersData : []);
            setWarehouses(Array.isArray(warehousesData) ? warehousesData : []);
            setProducts(Array.isArray(productsData) ? productsData : []);
        } catch (error) {
            setErrorMessage(error.message);
        } finally {
            setLoading(false);
        }
    };

    const refreshOrders = async () => {
        const response = await fetch(`${API}/purchase-orders`);

        if (!response.ok) {
            throw new Error(
                "Action completed, but the order list could not be refreshed."
            );
        }

        const data = await response.json();
        setOrders(Array.isArray(data) ? data : data.content || []);
    };

    const handleFormChange = (event) => {
        const { name, value } = event.target;

        setFormData((previous) => ({
            ...previous,
            [name]: value
        }));
    };

    const handleItemChange = (index, field, value) => {
        setFormData((previous) => ({
            ...previous,
            items: previous.items.map((item, itemIndex) =>
                itemIndex === index
                    ? { ...item, [field]: value }
                    : item
            )
        }));
    };

    const addItem = () => {
        setFormData((previous) => ({
            ...previous,
            items: [...previous.items, emptyItem()]
        }));
    };

    const removeItem = (index) => {
        setFormData((previous) => ({
            ...previous,
            items: previous.items.filter((_, itemIndex) => itemIndex !== index)
        }));
    };

    const getLineTotal = (item) =>
        (Number(item.quantity) || 0) * (Number(item.unitPrice) || 0);

    const totalAmount = formData.items.reduce(
        (total, item) => total + getLineTotal(item),
        0
    );

    const resetForm = () => {
        setFormData({
            supplierId: "",
            warehouseId: "",
            expectedDeliveryDate: "",
            notes: "",
            items: [emptyItem()]
        });
    };

    const handleCreateOrder = async (event) => {
        event.preventDefault();
        setSuccessMessage("");
        setErrorMessage("");

        if (!formData.supplierId || !formData.warehouseId) {
            setErrorMessage("Please select a supplier and warehouse.");
            return;
        }

        if (activeSuppliers.length === 0) {
            setErrorMessage("No active suppliers are available.");
            return;
        }

        if (activeWarehouses.length === 0) {
            setErrorMessage("No active warehouses are available.");
            return;
        }

        const invalidItem = formData.items.some(
            (item) =>
                !item.productId ||
                Number(item.quantity) <= 0 ||
                item.unitPrice === "" ||
                Number(item.unitPrice) < 0
        );

        if (invalidItem) {
            setErrorMessage(
                "Select a product and enter a valid quantity and unit price for every item."
            );
            return;
        }

        setSaving(true);

        const payload = {
            supplierId: Number(formData.supplierId),
            warehouseId: Number(formData.warehouseId),
            expectedDeliveryDate: formData.expectedDeliveryDate || null,
            notes: formData.notes.trim(),
            items: formData.items.map((item) => ({
                productId: Number(item.productId),
                quantity: Number(item.quantity),
                unitPrice: Number(item.unitPrice)
            }))
        };

        try {
            const response = await fetch(`${API}/purchase-orders`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(payload)
            });

            if (!response.ok) {
                throw new Error(
                    await readError(response, "Unable to create purchase order.")
                );
            }

            const createdOrder = await response.json();

            setSuccessMessage(
                `Purchase order ${createdOrder.orderNumber || ""} created successfully.`
            );

            resetForm();
            await refreshOrders();
        } catch (error) {
            setErrorMessage(error.message);
        } finally {
            setSaving(false);
        }
    };

    // Handles ordering, receiving and cancellation.
    const handleOrderAction = async (orderId, action, successText) => {
        if (action === "cancel") {
            const confirmed = window.confirm(
                "Are you sure you want to cancel this purchase order? This action cannot be undone."
            );

            if (!confirmed) {
                return;
            }
        }

        setSuccessMessage("");
        setErrorMessage("");

        try {
            const response = await fetch(
                `${API}/purchase-orders/${orderId}/${action}`,
                { method: "PATCH" }
            );

            if (!response.ok) {
                throw new Error(
                    await readError(
                        response,
                        `Unable to ${action} purchase order.`
                    )
                );
            }

            setSuccessMessage(successText);
            setSelectedOrder(null);
            await refreshOrders();
        } catch (error) {
            setErrorMessage(error.message);
        }
    };

    const viewOrderDetails = async (orderId) => {
        setErrorMessage("");
        setSuccessMessage("");

        try {
            const response = await fetch(
                `${API}/purchase-orders/${orderId}`
            );

            if (!response.ok) {
                throw new Error(
                    await readError(
                        response,
                        "Unable to load purchase order details."
                    )
                );
            }

            const data = await response.json();
            setSelectedOrder(data);
        } catch (error) {
            setErrorMessage(error.message);
        }
    };

    const formatCurrency = (amount) =>
        `₹${Number(amount || 0).toLocaleString("en-IN", {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        })}`;

    const formatDate = (date) => {
        if (!date) return "—";

        return new Date(
            `${date}`.length === 10 ? `${date}T00:00:00` : date
        ).toLocaleDateString("en-IN");
    };

    const statusClass = (status) =>
        `po-status po-status-${String(status || "").toLowerCase()}`;

    return (
        <div className="products-container">
            <div className="products-header">
                <h2>Purchase Order Management</h2>
                <p>
                    Create purchase orders, track their status, and receive stock.
                </p>
            </div>

            {successMessage && (
                <div className="success-message">{successMessage}</div>
            )}

            {errorMessage && (
                <div className="error-message">{errorMessage}</div>
            )}

            <div className="product-form-container">
                <h3>Create Purchase Order</h3>

                <form className="product-form" onSubmit={handleCreateOrder}>
                    <div className="form-group">
                        <label>Supplier</label>
                        <select
                            name="supplierId"
                            value={formData.supplierId}
                            onChange={handleFormChange}
                            required
                        >
                            <option value="">Select supplier</option>
                            {activeSuppliers.map((supplier) => (
                                <option key={supplier.id} value={supplier.id}>
                                    {supplier.name} ({supplier.code})
                                </option>
                            ))}
                        </select>
                    </div>

                    <div className="form-group">
                        <label>Warehouse</label>
                        <select
                            name="warehouseId"
                            value={formData.warehouseId}
                            onChange={handleFormChange}
                            required
                        >
                            <option value="">Select warehouse</option>
                            {activeWarehouses.map((warehouse) => (
                                <option key={warehouse.id} value={warehouse.id}>
                                    {warehouse.name} ({warehouse.code})
                                </option>
                            ))}
                        </select>
                    </div>

                    <div className="form-group">
                        <label>Expected Delivery Date</label>
                        <input
                            type="date"
                            name="expectedDeliveryDate"
                            value={formData.expectedDeliveryDate}
                            onChange={handleFormChange}
                        />
                    </div>

                    <div className="form-group">
                        <label>Notes</label>
                        <input
                            type="text"
                            name="notes"
                            value={formData.notes}
                            onChange={handleFormChange}
                            placeholder="Optional notes"
                        />
                    </div>

                    <div className="po-items-section">
                        <h4>Order Items</h4>

                        {formData.items.map((item, index) => (
                            <div className="po-item-row" key={index}>
                                <div className="form-group">
                                    <label>Product</label>
                                    <select
                                        value={item.productId}
                                        onChange={(event) =>
                                            handleItemChange(
                                                index,
                                                "productId",
                                                event.target.value
                                            )
                                        }
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

                                <div className="form-group">
                                    <label>Quantity</label>
                                    <input
                                        type="number"
                                        min="1"
                                        step="1"
                                        value={item.quantity}
                                        onChange={(event) =>
                                            handleItemChange(
                                                index,
                                                "quantity",
                                                event.target.value
                                            )
                                        }
                                        required
                                    />
                                </div>

                                <div className="form-group">
                                    <label>Unit Price (₹)</label>
                                    <input
                                        type="number"
                                        min="0"
                                        step="0.01"
                                        value={item.unitPrice}
                                        onChange={(event) =>
                                            handleItemChange(
                                                index,
                                                "unitPrice",
                                                event.target.value
                                            )
                                        }
                                        required
                                    />
                                </div>

                                <div className="form-group">
                                    <label>Line Total</label>
                                    <input
                                        value={formatCurrency(getLineTotal(item))}
                                        readOnly
                                    />
                                </div>

                                <button
                                    type="button"
                                    className="delete-btn"
                                    onClick={() => removeItem(index)}
                                    disabled={formData.items.length === 1}
                                >
                                    Remove
                                </button>
                            </div>
                        ))}

                        <button
                            type="button"
                            className="cancel-btn"
                            onClick={addItem}
                        >
                            + Add Another Item
                        </button>
                    </div>

                    <div className="po-total">
                        <strong>
                            Order Total: {formatCurrency(totalAmount)}
                        </strong>
                    </div>

                    <div className="form-actions">
                        <button
                            type="submit"
                            className="primary-btn"
                            disabled={saving || loading}
                        >
                            {saving ? "Creating..." : "Create Purchase Order"}
                        </button>

                        <button
                            type="button"
                            className="cancel-btn"
                            onClick={resetForm}
                        >
                            Reset
                        </button>
                    </div>
                </form>
            </div>

            {selectedOrder && (
                <div className="products-table-container po-details">
                    <div className="po-details-header">
                        <h3>
                            Order Details: {selectedOrder.orderNumber}
                        </h3>

                        <button
                            className="cancel-btn"
                            onClick={() => setSelectedOrder(null)}
                        >
                            Close
                        </button>
                    </div>

                    <div className="po-details-grid">
                        <p>
                            <strong>Supplier:</strong>{" "}
                            {selectedOrder.supplierName}
                        </p>
                        <p>
                            <strong>Warehouse:</strong>{" "}
                            {selectedOrder.warehouseName}
                        </p>
                        <p>
                            <strong>Status:</strong> {selectedOrder.status}
                        </p>
                        <p>
                            <strong>Order Date:</strong>{" "}
                            {formatDate(selectedOrder.orderDate)}
                        </p>
                        <p>
                            <strong>Expected Delivery:</strong>{" "}
                            {formatDate(selectedOrder.expectedDeliveryDate)}
                        </p>
                        <p>
                            <strong>Received At:</strong>{" "}
                            {formatDate(selectedOrder.receivedAt)}
                        </p>
                        <p>
                            <strong>Notes:</strong>{" "}
                            {selectedOrder.notes || "—"}
                        </p>
                    </div>

                    <div className="table-responsive">
                        <table className="products-table">
                            <thead>
                                <tr>
                                    <th>Product</th>
                                    <th>SKU</th>
                                    <th>Quantity</th>
                                    <th>Unit Price</th>
                                    <th>Line Total</th>
                                </tr>
                            </thead>

                            <tbody>
                                {(selectedOrder.items || []).map((item) => (
                                    <tr key={item.id}>
                                        <td>{item.productName}</td>
                                        <td>{item.productSku}</td>
                                        <td>{item.quantity}</td>
                                        <td>{formatCurrency(item.unitPrice)}</td>
                                        <td>{formatCurrency(item.lineTotal)}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>

                    <p className="po-total">
                        <strong>
                            Total: {formatCurrency(selectedOrder.totalAmount)}
                        </strong>
                    </p>
                </div>
            )}

            <div className="products-table-container">
                <h3>Purchase Orders</h3>

                {loading ? (
                    <p>Loading purchase orders...</p>
                ) : (
                    <div className="table-responsive">
                        <table className="products-table">
                            <thead>
                                <tr>
                                    <th>Order Number</th>
                                    <th>Supplier</th>
                                    <th>Warehouse</th>
                                    <th>Order Date</th>
                                    <th>Expected Delivery</th>
                                    <th>Total Amount</th>
                                    <th>Status</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>

                            <tbody>
                                {orders.length === 0 ? (
                                    <tr>
                                        <td colSpan="8">
                                            No purchase orders found.
                                        </td>
                                    </tr>
                                ) : (
                                    orders.map((order) => (
                                        <tr key={order.id}>
                                            <td>{order.orderNumber}</td>
                                            <td>{order.supplierName}</td>
                                            <td>{order.warehouseName}</td>
                                            <td>{formatDate(order.orderDate)}</td>
                                            <td>
                                                {formatDate(order.expectedDeliveryDate)}
                                            </td>
                                            <td>
                                                {formatCurrency(order.totalAmount)}
                                            </td>
                                            <td>
                                                <span className={statusClass(order.status)}>
                                                    {order.status}
                                                </span>
                                            </td>
                                            <td>
                                                <div className="action-buttons">
                                                    <button
                                                        className="edit-btn"
                                                        onClick={() =>
                                                            viewOrderDetails(order.id)
                                                        }
                                                    >
                                                        View
                                                    </button>

                                                    {order.status === "DRAFT" && (
                                                        <>
                                                            <button
                                                                className="primary-btn"
                                                                onClick={() =>
                                                                    handleOrderAction(
                                                                        order.id,
                                                                        "order",
                                                                        "Purchase order marked as ordered."
                                                                    )
                                                                }
                                                            >
                                                                Mark Ordered
                                                            </button>

                                                            <button
                                                                className="delete-btn"
                                                                onClick={() =>
                                                                    handleOrderAction(
                                                                        order.id,
                                                                        "cancel",
                                                                        "Purchase order cancelled."
                                                                    )
                                                                }
                                                            >
                                                                Cancel
                                                            </button>
                                                        </>
                                                    )}

                                                    {order.status === "ORDERED" && (
                                                        <>
                                                            <button
                                                                className="primary-btn"
                                                                onClick={() =>
                                                                    handleOrderAction(
                                                                        order.id,
                                                                        "receive",
                                                                        "Purchase order received. Inventory has been updated."
                                                                    )
                                                                }
                                                            >
                                                                Receive
                                                            </button>

                                                            <button
                                                                className="delete-btn"
                                                                onClick={() =>
                                                                    handleOrderAction(
                                                                        order.id,
                                                                        "cancel",
                                                                        "Purchase order cancelled."
                                                                    )
                                                                }
                                                            >
                                                                Cancel
                                                            </button>
                                                        </>
                                                    )}
                                                </div>
                                            </td>
                                        </tr>
                                    ))
                                )}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>
        </div>
    );
}

export default PurchaseOrders;
