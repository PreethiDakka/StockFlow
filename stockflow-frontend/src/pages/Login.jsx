
import { useState } from "react";
import { useAuth } from "../context/AuthContext";

function Login({ onSwitch }) {
    const { login } = useAuth();

    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);

    const handleSubmit = async (event) => {
        event.preventDefault();

        setError("");
        setLoading(true);

        try {
            await login(username.trim(), password);
        } catch (err) {
            setError(err.message || "Unable to login.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="auth-page">
            <div className="auth-card">
                <h1>StockFlow</h1>
                <h2>Welcome Back</h2>
                <p className="auth-subtitle">
                    Sign in to manage your inventory.
                </p>

                {error && (
                    <div className="auth-error">
                        {error}
                    </div>
                )}

                <form onSubmit={handleSubmit}>
                    <div className="auth-form-group">
                        <label htmlFor="login-username">
                            Username
                        </label>

                        <input
                            id="login-username"
                            type="text"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            placeholder="Enter your username"
                            autoComplete="username"
                            required
                        />
                    </div>

                    <div className="auth-form-group">
                        <label htmlFor="login-password">
                            Password
                        </label>

                        <input
                            id="login-password"
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            placeholder="Enter your password"
                            autoComplete="current-password"
                            required
                        />
                    </div>

                    <button
                        type="submit"
                        className="auth-submit-btn"
                        disabled={loading}
                    >
                        {loading ? "Signing in..." : "Login"}
                    </button>
                </form>

                <p className="auth-switch">
                    Don't have an account?{" "}
                    <button
                        type="button"
                        onClick={() => onSwitch("register")}
                    >
                        Register
                    </button>
                </p>
            </div>
        </div>
    );
}

export default Login;
