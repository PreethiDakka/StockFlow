
import { useEffect, useState } from "react";
import { apiFetch as fetch } from "./api/api";

function Products() {
    const [products, setProducts] = useState([]);
    const [formData, setFormData] = useState({
        name: "",
        sku: "",
        description: "",
        price: "",
        reorderLevel: ""
    });

    const [editingId, setEditingId] = useState(null);
    const [successMessage, setSuccessMessage] = useState("");
    const [errorMessage, setErrorMessage] = useState("");
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        fetchProducts();
    }, []);

    const fetchProducts = async () => {
        try {
            const response = await fetch("/api/v1/products");

            if (!response.ok) {
                throw new Error("Unable to fetch products");
            }

            const data = await response.json();
            setProducts(data);
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
            sku: "",
            description: "",
            price: "",
            reorderLevel: ""
        });

        setEditingId(null);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        setSuccessMessage("");
        setErrorMessage("");
        setLoading(true);

        const productData = {
            ...formData,
            price: Number(formData.price),
            reorderLevel: Number(formData.reorderLevel)
        };

        try {
            const url = editingId
                ? `/api/v1/products/${editingId}`
                : "/api/v1/products";

            const response = await fetch(url, {
                method: editingId ? "PUT" : "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(productData)
            });

            if (!response.ok) {
                const error = await response.json().catch(() => null);
                throw new Error(
                    error?.message || "Unable to save product"
                );
            }

            setSuccessMessage(
                editingId
                    ? "Product updated successfully!"
                    : "Product added successfully!"
            );

            resetForm();
            await fetchProducts();

        } catch (error) {
            setErrorMessage(error.message);
        } finally {
            setLoading(false);
        }
    };

    const handleEdit = (product) => {
        setEditingId(product.id);

        setFormData({
            name: product.name,
            sku: product.sku,
            description: product.description || "",
            price: product.price,
            reorderLevel: product.reorderLevel
        });

        setSuccessMessage("");
        setErrorMessage("");

        window.scrollTo({
            top: 0,
            behavior: "smooth"
        });
    };

    // SOFT DEACTIVATION - Does not permanently delete the product
    const handleDeactivate = async (id) => {
        const confirmed = window.confirm(
            "Are you sure you want to deactivate this product?"
        );

        if (!confirmed) {
            return;
        }

        setSuccessMessage("");
        setErrorMessage("");

        try {
            const response = await fetch(
                `/api/v1/products/${id}/deactivate`,
                {
                    method: "PATCH"
                }
            );

            if (!response.ok) {
                const error = await response.json().catch(() => null);

                throw new Error(
                    error?.message || "Unable to deactivate product"
                );
            }

            setSuccessMessage("Product deactivated successfully!");

            if (editingId === id) {
                resetForm();
            }

            await fetchProducts();

        } catch (error) {
            setErrorMessage(error.message);
        }
    };

    return (
        <div className="products-container">

            <div className="products-header">
                <h2>Product Management</h2>
                <p>Manage your inventory products.</p>
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
                    {editingId ? "Edit Product" : "Add New Product"}
                </h3>

                <form className="product-form" onSubmit={handleSubmit}>

                    <div className="form-group">
                        <label>Product Name</label>
                        <input
                            type="text"
                            name="name"
                            value={formData.name}
                            onChange={handleChange}
                            placeholder="Enter product name"
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label>SKU</label>
                        <input
                            type="text"
                            name="sku"
                            value={formData.sku}
                            onChange={handleChange}
                            placeholder="Enter SKU"
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label>Description</label>
                        <textarea
                            name="description"
                            value={formData.description}
                            onChange={handleChange}
                            placeholder="Enter product description"
                        />
                    </div>

                    <div className="form-group">
                        <label>Price</label>
                        <input
                            type="number"
                            name="price"
                            value={formData.price}
                            onChange={handleChange}
                            min="0.01"
                            step="0.01"
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label>Reorder Level</label>
                        <input
                            type="number"
                            name="reorderLevel"
                            value={formData.reorderLevel}
                            onChange={handleChange}
                            min="0"
                            required
                        />
                    </div>

                    <div className="form-actions">
                        <button
                            type="submit"
                            className="primary-btn"
                            disabled={loading}
                        >
                            {loading
                                ? "Saving..."
                                : editingId
                                    ? "Update Product"
                                    : "Add Product"}
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
                <h3>Products List</h3>

                <div className="table-responsive">
                    <table className="products-table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Name</th>
                                <th>SKU</th>
                                <th>Description</th>
                                <th>Price</th>
                                <th>Reorder Level</th>
                                <th>Actions</th>
                            </tr>
                        </thead>

                        <tbody>
                            {products.length === 0 ? (
                                <tr>
                                    <td colSpan="7">
                                        No active products found.
                                    </td>
                                </tr>
                            ) : (
                                products.map((product) => (
                                    <tr key={product.id}>
                                        <td>{product.id}</td>
                                        <td>{product.name}</td>
                                        <td>{product.sku}</td>
                                        <td>{product.description}</td>
                                        <td>
                                            ₹{Number(product.price).toFixed(2)}
                                        </td>
                                        <td>{product.reorderLevel}</td>

                                        <td>
                                            <div className="action-buttons">
                                                <button
                                                    className="edit-btn"
                                                    onClick={() =>
                                                        handleEdit(product)
                                                    }
                                                >
                                                    Edit
                                                </button>

                                                <button
                                                    className="delete-btn"
                                                    onClick={() =>
                                                        handleDeactivate(product.id)
                                                    }
                                                >
                                                    Deactivate
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

        </div>
    );
}

export default Products;
