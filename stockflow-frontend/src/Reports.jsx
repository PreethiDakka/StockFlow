import { useCallback, useEffect, useMemo, useState } from "react";
import "./Reports.css";
import { apiFetch as fetch } from "./api/api";

function getLocalDateString(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
}

function downloadCsv(filename, rows) {
    if (!rows.length) {
        return;
    }

    const escapeCsv = (value) => {
        let text = String(value ?? "");
        // Prevent spreadsheet formula injection when opening exported CSV files.
        if (/^[=+\-@\t\r]/.test(text)) {
            text = `'${text}`;
        }
        return `"${text.replaceAll('"', '""')}"`;
    };

    const csv = rows.map((row) => row.map(escapeCsv).join(",")).join("\r\n");
    const blob = new Blob(["\uFEFF", csv], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
}

function Reports() {
    const today = new Date();
    const thirtyDaysAgo = new Date();
    thirtyDaysAgo.setDate(today.getDate() - 29);

    const [inventory, setInventory] = useState([]);
    const [warehouses, setWarehouses] = useState([]);
    const [movements, setMovements] = useState([]);
    const [startDate, setStartDate] = useState(getLocalDateString(thirtyDaysAgo));
    const [endDate, setEndDate] = useState(getLocalDateString(today));
    const [loading, setLoading] = useState(true);
    const [errorMessage, setErrorMessage] = useState("");

    const fetchAllMovements = async () => {
        const allMovements = [];
        let page = 0;
        let totalPages = 1;

        while (page < totalPages) {
            const response = await fetch(
                `/api/v1/stock-movements?page=${page}&size=100&sort=createdAt,desc`
            );

            if (!response.ok) {
                throw new Error(`Unable to load stock movements (HTTP ${response.status}).`);
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
                throw new Error("Unexpected stock movement response format.");
            }
        }

        return allMovements;
    };

    const loadReportData = useCallback(async () => {
        setLoading(true);
        setErrorMessage("");

        try {
            const [inventoryResponse, warehouseResponse, movementData] = await Promise.all([
                fetch("/api/v1/inventory"),
                fetch("/api/v1/warehouses"),
                fetchAllMovements()
            ]);

            if (!inventoryResponse.ok) {
                throw new Error(`Unable to load inventory (HTTP ${inventoryResponse.status}).`);
            }
            if (!warehouseResponse.ok) {
                throw new Error(`Unable to load warehouses (HTTP ${warehouseResponse.status}).`);
            }

            const [inventoryData, warehouseData] = await Promise.all([
                inventoryResponse.json(),
                warehouseResponse.json()
            ]);

            setInventory(Array.isArray(inventoryData) ? inventoryData : []);
            setWarehouses(
                Array.isArray(warehouseData)
                    ? warehouseData
                    : warehouseData.content || []
            );
            setMovements(movementData);
        } catch (error) {
            setErrorMessage(error.message || "Unable to load report data.");
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        loadReportData();
    }, [loadReportData]);

    const filteredMovements = useMemo(() => {
        return movements.filter((movement) => {
            if (!movement.createdAt) return false;

            const movementDate = new Date(movement.createdAt);
            if (Number.isNaN(movementDate.getTime())) return false;

            const date = getLocalDateString(movementDate);
            return (!startDate || date >= startDate) && (!endDate || date <= endDate);
        });
    }, [movements, startDate, endDate]);

    const lowStockItems = useMemo(() => {
        return inventory
            .filter((item) => {
                if (item.reorderLevel === null || item.reorderLevel === undefined) {
                    return false;
                }
                return Number(item.quantity || 0) <= Number(item.reorderLevel);
            })
            .sort((a, b) => Number(a.quantity || 0) - Number(b.quantity || 0));
    }, [inventory]);

    const totalQuantity = useMemo(
        () => inventory.reduce((total, item) => total + Number(item.quantity || 0), 0),
        [inventory]
    );

    const movementSummary = useMemo(() => {
        const types = ["STOCK_IN", "STOCK_OUT", "TRANSFER_IN", "TRANSFER_OUT", "ADJUSTMENT"];

        return types.map((type) => {
            const matching = filteredMovements.filter((item) => item.movementType === type);
            return {
                type,
                count: matching.length,
                quantity: matching.reduce((total, item) => total + Number(item.quantity || 0), 0)
            };
        });
    }, [filteredMovements]);

    const warehouseSummary = useMemo(() => {
        const summaryById = new Map();

        warehouses.forEach((warehouse) => {
            summaryById.set(Number(warehouse.id), {
                warehouseId: warehouse.id,
                warehouseName: warehouse.name,
                totalQuantity: 0,
                inventoryRecords: 0,
                lowStockRecords: 0
            });
        });

        inventory.forEach((item) => {
            const id = Number(item.warehouseId);
            if (!summaryById.has(id)) {
                summaryById.set(id, {
                    warehouseId: item.warehouseId,
                    warehouseName: item.warehouseName || "Unknown warehouse",
                    totalQuantity: 0,
                    inventoryRecords: 0,
                    lowStockRecords: 0
                });
            }

            const summary = summaryById.get(id);
            summary.totalQuantity += Number(item.quantity || 0);
            summary.inventoryRecords += 1;

            if (
                item.reorderLevel !== null &&
                item.reorderLevel !== undefined &&
                Number(item.quantity || 0) <= Number(item.reorderLevel)
            ) {
                summary.lowStockRecords += 1;
            }
        });

        return Array.from(summaryById.values()).sort(
            (a, b) => b.totalQuantity - a.totalQuantity
        );
    }, [inventory, warehouses]);

    const formatQuantity = (value) =>
        Number(value || 0).toLocaleString(undefined, { maximumFractionDigits: 3 });

    const formatMovementType = (type) =>
        String(type || "").replaceAll("_", " ");

    const formatDate = (value) => {
        if (!value) return "—";
        const date = new Date(value);
        return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
    };

    const exportMovements = () => {
        const rows = [
            ["Movement ID", "Date & Time", "Product", "SKU", "Warehouse", "Movement Type", "Quantity", "Reference", "Remarks"],
            ...filteredMovements.map((item) => [
                item.id,
                formatDate(item.createdAt),
                item.productName,
                item.productSku,
                item.warehouseName,
                item.movementType,
                item.quantity,
                item.reference,
                item.remarks
            ])
        ];
        downloadCsv("stockflow-stock-movements.csv", rows);
    };

    const exportLowStock = () => {
        const rows = [
            ["Inventory ID", "Product", "SKU", "Warehouse", "Available Quantity", "Reorder Level"],
            ...lowStockItems.map((item) => [
                item.id,
                item.productName,
                item.productSku,
                item.warehouseName,
                item.quantity,
                item.reorderLevel
            ])
        ];
        downloadCsv("stockflow-low-stock-report.csv", rows);
    };

    return (
        <div className="reports-container">
            <div className="reports-header">
                <div>
                    <h2>Reports &amp; Analytics</h2>
                    <p>Review current inventory levels, warehouse distribution, and stock movement activity.</p>
                </div>
                <button type="button" className="reports-refresh-btn" onClick={loadReportData} disabled={loading}>
                    {loading ? "Loading..." : "Refresh Reports"}
                </button>
            </div>

            {errorMessage && <div className="error-message">{errorMessage}</div>}

            <section className="reports-stat-grid">
                <article className="reports-stat-card">
                    <span>Total Stock Quantity</span>
                    <strong>{loading ? "—" : formatQuantity(totalQuantity)}</strong>
                    <small>Across all inventory records</small>
                </article>
                <article className="reports-stat-card">
                    <span>Inventory Records</span>
                    <strong>{loading ? "—" : inventory.length}</strong>
                    <small>Product and warehouse combinations</small>
                </article>
                <article className="reports-stat-card reports-warning-card">
                    <span>Low Stock Records</span>
                    <strong>{loading ? "—" : lowStockItems.length}</strong>
                    <small>At or below reorder level</small>
                </article>
                <article className="reports-stat-card">
                    <span>Movements in Date Range</span>
                    <strong>{loading ? "—" : filteredMovements.length}</strong>
                    <small>All movement types included</small>
                </article>
            </section>

            <section className="reports-panel">
                <div className="reports-panel-heading">
                    <div>
                        <h3>Stock Movement Summary</h3>
                        <p>Movement totals for the selected date range.</p>
                    </div>
                </div>

                <div className="reports-date-filters">
                    <div>
                        <label htmlFor="reports-start-date">From</label>
                        <input
                            id="reports-start-date"
                            type="date"
                            value={startDate}
                            max={endDate || undefined}
                            onChange={(event) => setStartDate(event.target.value)}
                        />
                    </div>
                    <div>
                        <label htmlFor="reports-end-date">To</label>
                        <input
                            id="reports-end-date"
                            type="date"
                            value={endDate}
                            min={startDate || undefined}
                            onChange={(event) => setEndDate(event.target.value)}
                        />
                    </div>
                    <button type="button" className="reports-export-btn" onClick={exportMovements} disabled={loading || filteredMovements.length === 0}>
                        Export Movements CSV
                    </button>
                </div>

                <div className="reports-table-wrap">
                    <table className="reports-table">
                        <thead>
                            <tr>
                                <th>Movement Type</th>
                                <th>Record Count</th>
                                <th>Total Quantity</th>
                            </tr>
                        </thead>
                        <tbody>
                            {movementSummary.map((item) => (
                                <tr key={item.type}>
                                    <td>{formatMovementType(item.type)}</td>
                                    <td>{item.count}</td>
                                    <td>{formatQuantity(item.quantity)}</td>
                                </tr>
                            ))}
                            {movementSummary.every((item) => item.count === 0) && (
                                <tr><td colSpan="3">No movements found for this date range.</td></tr>
                            )}
                        </tbody>
                    </table>
                </div>
                <p className="reports-note">
                    Transfers are shown as separate transfer-out and transfer-in records. Do not add both quantities together when calculating net stock change.
                </p>
            </section>

            <section className="reports-panel">
                <div className="reports-panel-heading">
                    <div>
                        <h3>Warehouse-wise Inventory</h3>
                        <p>Current stock distribution by warehouse.</p>
                    </div>
                </div>
                <div className="reports-table-wrap">
                    <table className="reports-table">
                        <thead>
                            <tr>
                                <th>Warehouse</th>
                                <th>Inventory Records</th>
                                <th>Total Quantity</th>
                                <th>Low Stock Records</th>
                            </tr>
                        </thead>
                        <tbody>
                            {loading ? (
                                <tr><td colSpan="4">Loading warehouse report...</td></tr>
                            ) : warehouseSummary.length === 0 ? (
                                <tr><td colSpan="4">No warehouse inventory data found.</td></tr>
                            ) : (
                                warehouseSummary.map((item) => (
                                    <tr key={item.warehouseId}>
                                        <td>{item.warehouseName}</td>
                                        <td>{item.inventoryRecords}</td>
                                        <td>{formatQuantity(item.totalQuantity)}</td>
                                        <td>{item.lowStockRecords}</td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>
            </section>

            <section className="reports-panel">
                <div className="reports-panel-heading reports-low-stock-heading">
                    <div>
                        <h3>Low Stock Report</h3>
                        <p>Inventory records that have reached or fallen below their reorder level.</p>
                    </div>
                    <button type="button" className="reports-export-btn" onClick={exportLowStock} disabled={loading || lowStockItems.length === 0}>
                        Export Low Stock CSV
                    </button>
                </div>
                <div className="reports-table-wrap">
                    <table className="reports-table">
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
                                <tr><td colSpan="5">Loading low-stock report...</td></tr>
                            ) : lowStockItems.length === 0 ? (
                                <tr><td colSpan="5">No low-stock records found.</td></tr>
                            ) : (
                                lowStockItems.map((item) => (
                                    <tr key={item.id}>
                                        <td>{item.productName}</td>
                                        <td>{item.productSku}</td>
                                        <td>{item.warehouseName}</td>
                                        <td>{formatQuantity(item.quantity)}</td>
                                        <td>{formatQuantity(item.reorderLevel)}</td>
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

export default Reports;
