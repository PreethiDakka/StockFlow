import { useCallback, useEffect, useMemo, useState } from "react";
import "./Inventory.css";
import { apiFetch as fetch } from "./api/api";

function Inventory() {
    const [inventory, setInventory] = useState([]);
    const [warehouses, setWarehouses] = useState([]);
    const [productFilter, setProductFilter] = useState("all");
    const [warehouseFilter, setWarehouseFilter] = useState("all");
    const [stockFilter, setStockFilter] = useState("all");
    const [loading, setLoading] = useState(true);
    const [errorMessage, setErrorMessage] = useState("");

    const fetchInventoryData = useCallback(async () => {
        setLoading(true);
        setErrorMessage("");

        try {
            const responses = await Promise.all([
                fetch("/api/v1/inventory"),
                fetch("/api/v1/warehouses")
            ]);

            const failedResponse = responses.find((response) => !response.ok);
            if (failedResponse) {
                throw new Error(`Unable to load inventory data (HTTP ${failedResponse.status}).`);
            }

            const [inventoryData, warehousesData] = await Promise.all(
                responses.map((response) => response.json())
            );

            setInventory(Array.isArray(inventoryData) ? inventoryData : []);
            setWarehouses(Array.isArray(warehousesData) ? warehousesData : []);
        } catch (error) {
            setErrorMessage(error.message || "Unable to load inventory data.");
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchInventoryData();
    }, [fetchInventoryData]);

    const productOptions = useMemo(() => {
        const uniqueProducts = new Map();

        inventory.forEach((item) => {
            if (!uniqueProducts.has(Number(item.productId))) {
                uniqueProducts.set(Number(item.productId), {
                    id: item.productId,
                    name: item.productName,
                    sku: item.productSku
                });
            }
        });

        return Array.from(uniqueProducts.values());
    }, [inventory]);

    const inventoryWithStatus = useMemo(() => inventory.map((item) => {
        const quantity = Number(item.quantity || 0);
        const reorderLevel =
            item.reorderLevel !== null && item.reorderLevel !== undefined
                ? Number(item.reorderLevel)
                : null;

        let stockStatus = "Not available";
        if (reorderLevel !== null) {
            stockStatus = quantity <= reorderLevel ? "Low Stock" : "In Stock";
        }

        return { ...item, quantity, reorderLevel, stockStatus };
    }), [inventory]);

    const filteredInventory = useMemo(() => inventoryWithStatus.filter((item) => {
        const matchesProduct =
            productFilter === "all" || String(item.productId) === productFilter;
        const matchesWarehouse =
            warehouseFilter === "all" || String(item.warehouseId) === warehouseFilter;
        const matchesStock =
            stockFilter === "all" ||
            (stockFilter === "low" && item.stockStatus === "Low Stock") ||
            (stockFilter === "in" && item.stockStatus === "In Stock");

        return matchesProduct && matchesWarehouse && matchesStock;
    }), [inventoryWithStatus, productFilter, warehouseFilter, stockFilter]);

    const totalQuantity = inventoryWithStatus.reduce(
        (total, item) => total + item.quantity,
        0
    );

    const lowStockCount = inventoryWithStatus.filter(
        (item) => item.stockStatus === "Low Stock"
    ).length;

    const clearFilters = () => {
        setProductFilter("all");
        setWarehouseFilter("all");
        setStockFilter("all");
    };

    return (
        <div className="inventory-container">
            <div className="inventory-header">
                <div>
                    <h2>Inventory Management</h2>
                    <p>View available stock across all your warehouses.</p>
                </div>
                <button
                    type="button"
                    className="inventory-refresh-btn"
                    onClick={fetchInventoryData}
                    disabled={loading}
                >
                    {loading ? "Loading..." : "Refresh"}
                </button>
            </div>

            {errorMessage && <div className="error-message">{errorMessage}</div>}

            <div className="inventory-summary">
                <div className="inventory-summary-card">
                    <span>Total Inventory Records</span>
                    <strong>{loading ? "—" : inventory.length}</strong>
                </div>
                <div className="inventory-summary-card">
                    <span>Total Stock Quantity</span>
                    <strong>{loading ? "—" : totalQuantity.toLocaleString(undefined, { maximumFractionDigits: 3 })}</strong>
                </div>
                <div className="inventory-summary-card inventory-low-card">
                    <span>Low Stock Records</span>
                    <strong>{loading ? "—" : lowStockCount}</strong>
                </div>
            </div>

            <section className="inventory-filter-card">
                <h3>Filter Inventory</h3>
                <div className="inventory-filter-grid">
                    <div className="inventory-filter-group">
                        <label htmlFor="inventory-product-filter">Product</label>
                        <select
                            id="inventory-product-filter"
                            value={productFilter}
                            onChange={(event) => setProductFilter(event.target.value)}
                        >
                            <option value="all">All products</option>
                            {productOptions.map((product) => (
                                <option key={product.id} value={String(product.id)}>
                                    {product.name} ({product.sku})
                                </option>
                            ))}
                        </select>
                    </div>

                    <div className="inventory-filter-group">
                        <label htmlFor="inventory-warehouse-filter">Warehouse</label>
                        <select
                            id="inventory-warehouse-filter"
                            value={warehouseFilter}
                            onChange={(event) => setWarehouseFilter(event.target.value)}
                        >
                            <option value="all">All warehouses</option>
                            {warehouses.map((warehouse) => (
                                <option key={warehouse.id} value={String(warehouse.id)}>
                                    {warehouse.name}
                                </option>
                            ))}
                        </select>
                    </div>

                    <div className="inventory-filter-group">
                        <label htmlFor="inventory-stock-filter">Stock Status</label>
                        <select
                            id="inventory-stock-filter"
                            value={stockFilter}
                            onChange={(event) => setStockFilter(event.target.value)}
                        >
                            <option value="all">All statuses</option>
                            <option value="low">Low stock</option>
                            <option value="in">In stock</option>
                        </select>
                    </div>
                </div>
                <button type="button" className="inventory-clear-btn" onClick={clearFilters}>
                    Clear Filters
                </button>
            </section>

            <section className="inventory-list-section">
                <div className="inventory-list-heading">
                    <h3>Inventory Records</h3>
                    <span>{loading ? "Loading..." : `${filteredInventory.length} record(s) found`}</span>
                </div>

                <div className="inventory-table-wrap">
                    <table className="inventory-table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Product</th>
                                <th>SKU</th>
                                <th>Warehouse</th>
                                <th>Available Quantity</th>
                                <th>Reorder Level</th>
                                <th>Stock Status</th>
                            </tr>
                        </thead>
                        <tbody>
                            {loading ? (
                                <tr><td colSpan="7" className="inventory-empty">Loading inventory records...</td></tr>
                            ) : filteredInventory.length === 0 ? (
                                <tr><td colSpan="7" className="inventory-empty">No inventory records match your filters.</td></tr>
                            ) : (
                                filteredInventory.map((item) => (
                                    <tr key={item.id}>
                                        <td>{item.id}</td>
                                        <td>{item.productName}</td>
                                        <td>{item.productSku}</td>
                                        <td>{item.warehouseName}</td>
                                        <td>{item.quantity.toLocaleString(undefined, { maximumFractionDigits: 3 })}</td>
                                        <td>{item.reorderLevel === null ? "—" : item.reorderLevel}</td>
                                        <td>
                                            <span className={`inventory-status ${item.stockStatus === "Low Stock" ? "is-low" : item.stockStatus === "In Stock" ? "is-in-stock" : "is-unknown"}`}>
                                                {item.stockStatus}
                                            </span>
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>
            </section>
        </div>
    );
}

export default Inventory;
