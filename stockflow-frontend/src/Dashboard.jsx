import { useCallback, useEffect, useMemo, useState } from "react";
import "./Dashboard.css";
import { apiFetch as fetch } from "./api/api";

function Dashboard() {
    const [products, setProducts] = useState([]);
    const [warehouses, setWarehouses] = useState([]);
    const [inventory, setInventory] = useState([]);
    const [movements, setMovements] = useState([]);
    const [loading, setLoading] = useState(true);
    const [errorMessage, setErrorMessage] = useState("");

    const fetchDashboardData = useCallback(async () => {
        setLoading(true);
        setErrorMessage("");

        try {
            const endpoints = [
                "/api/v1/products",
                "/api/v1/warehouses",
                "/api/v1/inventory",
                "/api/v1/stock-movements?page=0&size=5&sort=createdAt,desc"
            ];

            const responses = await Promise.all(
                endpoints.map((url) => fetch(url))
            );

            const failedResponse = responses.find((response) => !response.ok);
            if (failedResponse) {
                throw new Error(
                    `Unable to load dashboard data (HTTP ${failedResponse.status}).`
                );
            }

            const [productsData, warehousesData, inventoryData, movementsData] =
                await Promise.all(responses.map((response) => response.json()));

            setProducts(Array.isArray(productsData) ? productsData : []);
            setWarehouses(Array.isArray(warehousesData) ? warehousesData : []);
            setInventory(Array.isArray(inventoryData) ? inventoryData : []);
            setMovements(
                Array.isArray(movementsData)
                    ? movementsData
                    : movementsData.content || []
            );
        } catch (error) {
            setErrorMessage(error.message || "Unable to load dashboard data.");
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchDashboardData();
    }, [fetchDashboardData]);

    const totalQuantity = useMemo(
        () => inventory.reduce((total, item) => total + Number(item.quantity || 0), 0),
        [inventory]
    );

    const lowStockItems = useMemo(() => {
        return inventory
            .map((item) => {
                if (item.reorderLevel === null || item.reorderLevel === undefined) {
                    return null;
                }

                const reorderLevel = Number(item.reorderLevel);
                const quantity = Number(item.quantity || 0);

                return quantity <= reorderLevel
                    ? { ...item, reorderLevel }
                    : null;
            })
            .filter(Boolean)
            .sort((a, b) => Number(a.quantity) - Number(b.quantity));
    }, [inventory]);

    const formatDate = (value) => {
        if (!value) return "—";
        const date = new Date(value);
        return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
    };

    const movementLabel = (type) =>
        String(type || "").replaceAll("_", " ");

    return (
        <div className="dashboard-container">
            <div className="dashboard-header">
                <div>
                    <h2>Dashboard Overview</h2>
                    <p>Monitor your stock, products, warehouses, and recent activity.</p>
                </div>
                <button
                    type="button"
                    className="dashboard-refresh-btn"
                    onClick={fetchDashboardData}
                    disabled={loading}
                >
                    {loading ? "Loading..." : "Refresh"}
                </button>
            </div>

            {errorMessage && <div className="error-message">{errorMessage}</div>}

            <div className="dashboard-stats">
                <article className="dashboard-stat-card">
                    <span className="dashboard-stat-label">Active Products</span>
                    <strong>{loading ? "—" : products.length}</strong>
                    <span className="dashboard-stat-note">Currently active products</span>
                </article>

                <article className="dashboard-stat-card">
                    <span className="dashboard-stat-label">Total Warehouses</span>
                    <strong>{loading ? "—" : warehouses.length}</strong>
                    <span className="dashboard-stat-note">Registered warehouse locations</span>
                </article>

                <article className="dashboard-stat-card">
                    <span className="dashboard-stat-label">Total Stock Quantity</span>
                    <strong>{loading ? "—" : totalQuantity.toLocaleString(undefined, { maximumFractionDigits: 3 })}</strong>
                    <span className="dashboard-stat-note">Combined quantity across inventory records</span>
                </article>

                <article className="dashboard-stat-card dashboard-alert-card">
                    <span className="dashboard-stat-label">Low Stock Records</span>
                    <strong>{loading ? "—" : lowStockItems.length}</strong>
                    <span className="dashboard-stat-note">At or below the product reorder level</span>
                </article>
            </div>

            <section className="dashboard-section">
                <div className="dashboard-section-heading">
                    <div>
                        <h3>Low Stock Alerts</h3>
                        <p>Inventory records that have reached the product reorder level.</p>
                    </div>
                </div>

                <div className="dashboard-table-wrap">
                    <table className="dashboard-table">
                        <thead>
                            <tr>
                                <th>Product</th>
                                <th>SKU</th>
                                <th>Warehouse</th>
                                <th>Available Quantity</th>
                                <th>Reorder Level</th>
                            </tr>
                        </thead>
                        <tbody>
                            {loading ? (
                                <tr><td colSpan="5" className="dashboard-empty">Loading low-stock records...</td></tr>
                            ) : lowStockItems.length === 0 ? (
                                <tr><td colSpan="5" className="dashboard-empty">No low-stock records found.</td></tr>
                            ) : (
                                lowStockItems.map((item) => (
                                    <tr key={item.id}>
                                        <td>{item.productName}</td>
                                        <td>{item.productSku}</td>
                                        <td>{item.warehouseName}</td>
                                        <td><span className="low-stock-quantity">{Number(item.quantity).toLocaleString()}</span></td>
                                        <td>{item.reorderLevel}</td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>
            </section>

            <section className="dashboard-section">
                <div className="dashboard-section-heading">
                    <div>
                        <h3>Recent Stock Movements</h3>
                        <p>Latest stock-in, stock-out, and transfer activity.</p>
                    </div>
                </div>

                <div className="dashboard-table-wrap">
                    <table className="dashboard-table">
                        <thead>
                            <tr>
                                <th>Date &amp; Time</th>
                                <th>Product</th>
                                <th>Warehouse</th>
                                <th>Movement Type</th>
                                <th>Quantity</th>
                                <th>Reference</th>
                            </tr>
                        </thead>
                        <tbody>
                            {loading ? (
                                <tr><td colSpan="6" className="dashboard-empty">Loading recent movements...</td></tr>
                            ) : movements.length === 0 ? (
                                <tr><td colSpan="6" className="dashboard-empty">No stock movements found.</td></tr>
                            ) : (
                                movements.map((movement) => (
                                    <tr key={movement.id}>
                                        <td>{formatDate(movement.createdAt)}</td>
                                        <td>{movement.productName}</td>
                                        <td>{movement.warehouseName}</td>
                                        <td>{movementLabel(movement.movementType)}</td>
                                        <td>{Number(movement.quantity).toLocaleString()}</td>
                                        <td>{movement.reference || "—"}</td>
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

export default Dashboard;
