
import { useEffect, useState } from "react";
import { apiFetch as fetch } from "./api/api";

function Warehouses() {
    const [warehouses, setWarehouses] = useState([]);

    const [formData, setFormData] = useState({
        name: "",
        code: "",
        address: "",
        city: "",
        state: "",
        country: ""
    });

    const [editingId, setEditingId] = useState(null);
    const [successMessage, setSuccessMessage] = useState("");
    const [errorMessage, setErrorMessage] = useState("");
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        fetchWarehouses();
    }, []);

    const fetchWarehouses = async () => {
        try {
            const response = await fetch("/api/v1/warehouses");

            if (!response.ok) {
                throw new Error("Unable to fetch warehouses");
            }

            const data = await response.json();
            setWarehouses(data);
        } catch (error) {
            setErrorMessage(error.message);
        }
    };

    const handleChange = (e) => {
        const { name, value } = e.target;

        setFormData((previous) => ({
            ...previous,
            [name]: value
        }));
    };

    const resetForm = () => {
        setFormData({
            name: "",
            code: "",
            address: "",
            city: "",
            state: "",
            country: ""
        });

        setEditingId(null);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        setSuccessMessage("");
        setErrorMessage("");
        setLoading(true);

        try {
            const url = editingId
                ? `/api/v1/warehouses/${editingId}`
                : "/api/v1/warehouses";

            const response = await fetch(url, {
                method: editingId ? "PUT" : "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(formData)
            });

            if (!response.ok) {
                const error = await response.json().catch(() => null);

                throw new Error(
                    error?.message || "Unable to save warehouse"
                );
            }

            setSuccessMessage(
                editingId
                    ? "Warehouse updated successfully!"
                    : "Warehouse added successfully!"
            );

            resetForm();
            await fetchWarehouses();

        } catch (error) {
            setErrorMessage(error.message);
        } finally {
            setLoading(false);
        }
    };

    const handleEdit = (warehouse) => {
        setEditingId(warehouse.id);

        setFormData({
            name: warehouse.name || "",
            code: warehouse.code || "",
            address: warehouse.address || "",
            city: warehouse.city || "",
            state: warehouse.state || "",
            country: warehouse.country || ""
        });

        setSuccessMessage("");
        setErrorMessage("");

        window.scrollTo({
            top: 0,
            behavior: "smooth"
        });
    };

    const handleDeactivate = async (id) => {
        const confirmed = window.confirm(
            "Are you sure you want to deactivate this warehouse?"
        );

        if (!confirmed) return;

        setSuccessMessage("");
        setErrorMessage("");

        try {
            const response = await fetch(
                `/api/v1/warehouses/${id}/deactivate`,
                { method: "PATCH" }
            );

            if (!response.ok) {
                const error = await response.json().catch(() => null);

                throw new Error(
                    error?.message || "Unable to deactivate warehouse"
                );
            }

            setSuccessMessage("Warehouse deactivated successfully!");

            if (editingId === id) {
                resetForm();
            }

            await fetchWarehouses();

        } catch (error) {
            setErrorMessage(error.message);
        }
    };

    const handleActivate = async (id) => {
        setSuccessMessage("");
        setErrorMessage("");

        try {
            const response = await fetch(
                `/api/v1/warehouses/${id}/activate`,
                { method: "PATCH" }
            );

            if (!response.ok) {
                const error = await response.json().catch(() => null);

                throw new Error(
                    error?.message || "Unable to activate warehouse"
                );
            }

            setSuccessMessage("Warehouse activated successfully!");

            await fetchWarehouses();

        } catch (error) {
            setErrorMessage(error.message);
        }
    };

    return (
        <div className="products-container">

            <div className="products-header">
                <h2>Warehouse Management</h2>
                <p>Manage your warehouses and their locations.</p>
            </div>

            {successMessage && (
                <div className="success-message">
                    {successMessage}
                </div>
            )}

            {errorMessage && (
                <div className="error-message">
                    {errorMessage}
                </div>
            )}

            <div className="product-form-container">
                <h3>
                    {editingId ? "Edit Warehouse" : "Add New Warehouse"}
                </h3>

                <form className="product-form" onSubmit={handleSubmit}>

                    {[
                        ["name", "Warehouse Name", "Enter warehouse name"],
                        ["code", "Warehouse Code", "Enter warehouse code"],
                        ["address", "Address", "Enter address"],
                        ["city", "City", "Enter city"],
                        ["state", "State", "Enter state"],
                        ["country", "Country", "Enter country"]
                    ].map(([name, label, placeholder]) => (
                        <div className="form-group" key={name}>
                            <label>{label}</label>

                            <input
                                type="text"
                                name={name}
                                value={formData[name]}
                                onChange={handleChange}
                                placeholder={placeholder}
                                required={[
                                    "name",
                                    "code",
                                    "city",
                                    "country"
                                ].includes(name)}
                            />
                        </div>
                    ))}

                    <div className="form-actions">
                        <button
                            type="submit"
                            className="primary-btn"
                            disabled={loading}
                        >
                            {loading
                                ? "Saving..."
                                : editingId
                                    ? "Update Warehouse"
                                    : "Add Warehouse"}
                        </button>

                        {editingId && (
                            <button
                                type="button"
                                className="cancel-btn"
                                onClick={resetForm}
                            >
                                Cancel
                            </button>
                        )}
                    </div>

                </form>
            </div>

            <div className="products-table-container">
                <h3>Warehouses List</h3>

                <div className="table-responsive">
                    <table className="products-table">

                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Name</th>
                                <th>Code</th>
                                <th>Address</th>
                                <th>City</th>
                                <th>State</th>
                                <th>Country</th>
                                <th>Status</th>
                                <th>Actions</th>
                            </tr>
                        </thead>

                        <tbody>
                            {warehouses.length === 0 ? (
                                <tr>
                                    <td colSpan="9">
                                        No warehouses found.
                                    </td>
                                </tr>
                            ) : (
                                warehouses.map((warehouse) => {
                                    const active = warehouse.active;

                                    return (
                                        <tr key={warehouse.id}>
                                            <td>{warehouse.id}</td>
                                            <td>{warehouse.name}</td>
                                            <td>{warehouse.code}</td>
                                            <td>{warehouse.address || "-"}</td>
                                            <td>{warehouse.city}</td>
                                            <td>{warehouse.state || "-"}</td>
                                            <td>{warehouse.country}</td>

                                            <td>
                                                <span
                                                    style={{
                                                        padding: "5px 10px",
                                                        borderRadius: "12px",
                                                        fontSize: "12px",
                                                        fontWeight: "600",
                                                        backgroundColor:
                                                            active === true
                                                                ? "#dcfce7"
                                                                : active === false
                                                                    ? "#fee2e2"
                                                                    : "#f3f4f6",
                                                        color:
                                                            active === true
                                                                ? "#166534"
                                                                : active === false
                                                                    ? "#991b1b"
                                                                    : "#374151"
                                                    }}
                                                >
                                                    {active === true
                                                        ? "Active"
                                                        : active === false
                                                            ? "Inactive"
                                                            : "Unknown"}
                                                </span>
                                            </td>

                                            <td>
                                                <div className="action-buttons">

                                                    <button
                                                        className="edit-btn"
                                                        onClick={() =>
                                                            handleEdit(warehouse)
                                                        }
                                                    >
                                                        Edit
                                                    </button>

                                                    {active === true ? (
                                                        <button
                                                            className="delete-btn"
                                                            onClick={() =>
                                                                handleDeactivate(
                                                                    warehouse.id
                                                                )
                                                            }
                                                        >
                                                            Deactivate
                                                        </button>
                                                    ) : active === false ? (
                                                        <button
                                                            className="edit-btn"
                                                            onClick={() =>
                                                                handleActivate(
                                                                    warehouse.id
                                                                )
                                                            }
                                                        >
                                                            Activate
                                                        </button>
                                                    ) : null}

                                                </div>
                                            </td>
                                        </tr>
                                    );
                                })
                            )}
                        </tbody>

                    </table>
                </div>
            </div>

        </div>
    );
}

export default Warehouses;
