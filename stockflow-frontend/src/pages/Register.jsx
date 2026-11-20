
import { useState } from "react";
import { useAuth } from "../context/AuthContext";

function Register({ onSwitch }) {
    const { register } = useAuth();

    const [formData, setFormData] = useState({
        username: "",
        email: "",
        password: "",
        confirmPassword: ""
    });

    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);

    const handleChange = (event) => {
        const { name, value } = event.target;

        setFormData((previous) => ({
            ...previous,
            [name]: value
        }));
    };

    const handleSubmit = async (event) => {
        event.preventDefault();

        setError("");

        if (formData.password !== formData.confirmPassword) {
            setError("Passwords do not match.");
            return;
        }

        setLoading(true);

        try {
            await register(
                formData.username.trim(),
                formData.email.trim(),
                formData.password
            );
        } catch (err) {
            setError(err.message || "Registration failed.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="auth-page">
            <div className="auth-card">
                <h1>StockFlow</h1>
                <h2>Create Account</h2>
                <p className="auth-subtitle">
                    Register to get started.
                </p>

                {error && (
                    <div className="auth-error">
                        {error}
                    </div>
                )}

                <form onSubmit={handleSubmit}>
                    <div className="auth-form-group">
                        <label htmlFor="register-username">
                            Username
                        </label>

                        <input
                            id="register-username"
                            name="username"
                            value={formData.username}
                            onChange={handleChange}
                            placeholder="Choose a username"
                            autoComplete="username"
                            required
                        />
                    </div>

                    <div className="auth-form-group">
                        <label htmlFor="register-email">
                            Email
                        </label>

                        <input
                            id="register-email"
                            name="email"
                            type="email"
                            value={formData.email}
                            onChange={handleChange}
                            placeholder="Enter your email"
                            autoComplete="email"
                            required
                        />
                    </div>

                    <div className="auth-form-group">
                        <label htmlFor="register-password">
                            Password
                        </label>

                        <input
                            id="register-password"
                            name="password"
                            type="password"
                            value={formData.password}
                            onChange={handleChange}
                            placeholder="Create a password"
                            autoComplete="new-password"
                            required
                        />
                    </div>

                    <div className="auth-form-group">
                        <label htmlFor="confirm-password">
                            Confirm Password
                        </label>

                        <input
                            id="confirm-password"
                            name="confirmPassword"
                            type="password"
                            value={formData.confirmPassword}
                            onChange={handleChange}
                            placeholder="Confirm your password"
                            autoComplete="new-password"
                            required
                        />
                    </div>

                    <button
                        type="submit"
                        className="auth-submit-btn"
                        disabled={loading}
                    >
                        {loading ? "Creating account..." : "Register"}
                    </button>
                </form>

                <p className="auth-switch">
                    Already have an account?{" "}
                    <button
                        type="button"
                        onClick={() => onSwitch("login")}
                    >
                        Login
                    </button>
                </p>
            </div>
        </div>
    );
}

export default Register;
