
import {
    createContext,
    useContext,
    useEffect,
    useState
} from "react";

import {
    apiFetch,
    AUTH_STORAGE_KEY
} from "../api/api";

const AuthContext = createContext(null);

async function readResponse(response) {
    const text = await response.text();

    if (!text) return null;

    try {
        return JSON.parse(text);
    } catch {
        return { message: text };
    }
}

function getErrorMessage(data, fallback) {
    return data?.message || data?.error || fallback;
}

export function AuthProvider({ children }) {
    const [user, setUser] = useState(() => {
        try {
            const stored = sessionStorage.getItem(AUTH_STORAGE_KEY);
            return stored ? JSON.parse(stored) : null;
        } catch {
            sessionStorage.removeItem(AUTH_STORAGE_KEY);
            return null;
        }
    });

    useEffect(() => {
        const handleUnauthorized = () => {
            sessionStorage.removeItem(AUTH_STORAGE_KEY);
            setUser(null);
        };

        window.addEventListener(
            "stockflow:unauthorized",
            handleUnauthorized
        );

        return () => {
            window.removeEventListener(
                "stockflow:unauthorized",
                handleUnauthorized
            );
        };
    }, []);

    const saveUser = (data) => {
        if (!data?.token) {
            throw new Error("Authentication token was not received.");
        }

        const authenticatedUser = {
            token: data.token,
            username: data.username
        };

        sessionStorage.setItem(
            AUTH_STORAGE_KEY,
            JSON.stringify(authenticatedUser)
        );

        setUser(authenticatedUser);
    };

    const login = async (username, password) => {
        const response = await apiFetch("/api/v1/auth/login", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ username, password })
        });

        const data = await readResponse(response);

        if (!response.ok) {
            throw new Error(
                getErrorMessage(data, "Login failed. Check your credentials.")
            );
        }

        saveUser(data);
    };

    const register = async (username, email, password) => {
        const response = await apiFetch("/api/v1/auth/register", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                username,
                email,
                password
            })
        });

        const data = await readResponse(response);

        if (!response.ok) {
            throw new Error(
                getErrorMessage(data, "Registration failed.")
            );
        }

        saveUser(data);
    };

    const logout = () => {
        sessionStorage.removeItem(AUTH_STORAGE_KEY);
        setUser(null);
    };

    return (
        <AuthContext.Provider
            value={{
                user,
                login,
                register,
                logout
            }}
        >
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const context = useContext(AuthContext);

    if (!context) {
        throw new Error(
            "useAuth must be used inside AuthProvider."
        );
    }

    return context;
}
