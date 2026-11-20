
import { useEffect, useState } from "react";
import { apiFetch as fetch } from "./api/api";

function Suppliers() {
    const [suppliers, setSuppliers] = useState([]);
    const [formData, setFormData] = useState({
        name: "",
        code: "",
        contactPerson: "",
        email: "",
        phone: "",
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
        fetchSuppliers();
    }, []);

    const fetchSuppliers = async () => {
        try {
            const response = await fetch("/api/v1/suppliers");

            if (!response.ok) {
                throw new Error("Unable to fetch suppliers");
            }

            const data = await response.json();
            setSuppliers(data);
        } catch (error) {
            setErrorMessage(error.message);
        }
    };

    const isActive = (supplier) =>
        supplier.isActive ?? supplier.active ?? supplier.is_active;

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
            contactPerson: "",
            email: "",
            phone: "",
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
                ? `/api/v1/suppliers/${editingId}`
                : "/api/v1/suppliers";

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
                    error?.message || "Unable to save supplier"
                );
            }

            setSuccessMessage(
                editingId
                    ? "Supplier updated successfully!"
                    : "Supplier added successfully!"
            );

            resetForm();
            await fetchSuppliers();

        } catch (error) {
            setErrorMessage(error.message);
        } finally {
            setLoading(false);
        }
    };

    const handleEdit = (supplier) => {
        setEditingId(supplier.id);

        setFormData({
            name: supplier.name || "",
            code: supplier.code || "",
            contactPerson: supplier.contactPerson || "",
            email: supplier.email || "",
            phone: supplier.phone || "",
            address: supplier.address || "",
            city: supplier.city || "",
            state: supplier.state || "",
            country: supplier.country || ""
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
            "Are you sure you want to deactivate this supplier?"
        );

        if (!confirmed) return;

        setSuccessMessage("");
        setErrorMessage("");

        try {
            const response = await fetch(
                `/api/v1/suppliers/${id}/deactivate`,
                { method: "PATCH" }
            );

            if (!response.ok) {
                const error = await response.json().catch(() => null);
                throw new Error(
                    error?.message || "Unable to deactivate supplier"
                );
            }

            setSuccessMessage("Supplier deactivated successfully!");

            if (editingId === id) {
                resetForm();
            }

            await fetchSuppliers();

        } catch (error) {
            setErrorMessage(error.message);
        }
    };

    const handleActivate = async (id) => {
        const confirmed = window.confirm(
            "Are you sure you want to activate this supplier?"
        );

        if (!confirmed) return;

        setSuccessMessage("");
        setErrorMessage("");

        try {
            const response = await fetch(
                `/api/v1/suppliers/${id}/activate`,
                { method: "PATCH" }
            );

            if (!response.ok) {
                const error = await response.json().catch(() => null);
                throw new Error(
                    error?.message || "Unable to activate supplier"
                );
            }

            setSuccessMessage("Supplier activated successfully!");

            await fetchSuppliers();

        } catch (error) {
            setErrorMessage(error.message);
        }
    };

    return (
        <div className="products-container supplier-page">

            <div className="products-header">
                <h2>Supplier Management</h2>
                <p>Manage your suppliers and their contact details.</p>
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
                <h3>{editingId ? "Edit Supplier" : "Add New Supplier"}</h3>

                <form className="product-form" onSubmit={handleSubmit}>
                    {[
                        ["name", "Supplier Name", "Enter supplier name"],
                        ["code", "Supplier Code", "Enter supplier code"],
                        ["contactPerson", "Contact Person", "Enter contact person"],
                        ["email", "Email", "Enter email address"],
                        ["phone", "Phone", "Enter phone number"],
                        ["address", "Address", "Enter address"],
                        ["city", "City", "Enter city"],
                        ["state", "State", "Enter state"],
                        ["country", "Country", "Enter country"]
                    ].map(([name, label, placeholder]) => (
                        <div className="form-group" key={name}>
                            <label>{label}</label>
                            <input
                                type={name === "email" ? "email" : "text"}
                                name={name}
                                value={formData[name]}
                                onChange={handleChange}
                                placeholder={placeholder}
                                required={["name", "code"].includes(name)}
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
                                    ? "Update Supplier"
                                    : "Add Supplier"}
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
                <h3>Suppliers List</h3>

                <div className="table-responsive">
                    <table className="products-table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Name</th>
                                <th>Code</th>
                                <th>Contact Person</th>
                                <th>Email</th>
                                <th>Phone</th>
                                <th>City</th>
                                <th>Status</th>
                                <th>Actions</th>
                            </tr>
                        </thead>

                        <tbody>
                            {suppliers.length === 0 ? (
                                <tr>
                                    <td colSpan="9">No suppliers found.</td>
                                </tr>
                            ) : (
                                suppliers.map((supplier) => {
                                    const active = isActive(supplier);

                                    return (
                                        <tr key={supplier.id}>
                                            <td>{supplier.id}</td>
                                            <td>{supplier.name}</td>
                                            <td>{supplier.code}</td>
                                            <td>{supplier.contactPerson}</td>
                                            <td>{supplier.email}</td>
                                            <td>{supplier.phone}</td>
                                            <td>{supplier.city}</td>

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
                                                        onClick={() => handleEdit(supplier)}
                                                    >
                                                        Edit
                                                    </button>

                                                    {active === true && (
                                                        <button
                                                            className="delete-btn"
                                                            onClick={() =>
                                                                handleDeactivate(supplier.id)
                                                            }
                                                        >
                                                            Deactivate
                                                        </button>
                                                    )}

                                                    {active === false && (
                                                        <button
                                                            className="primary-btn"
                                                            onClick={() =>
                                                                handleActivate(supplier.id)
                                                            }
                                                        >
                                                            Activate
                                                        </button>
                                                    )}
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

export default Suppliers;
