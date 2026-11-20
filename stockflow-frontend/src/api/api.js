
const AUTH_STORAGE_KEY = "stockflow_auth";

export async function apiFetch(url, options = {}) {
    const headers = new Headers(options.headers || {});

    let token = null;

    try {
        const storedAuth = sessionStorage.getItem(AUTH_STORAGE_KEY);
        token = storedAuth ? JSON.parse(storedAuth)?.token : null;
    } catch {
        sessionStorage.removeItem(AUTH_STORAGE_KEY);
    }

    if (token) {
        headers.set("Authorization", `Bearer ${token}`);
    }

    const response = await fetch(url, {
        ...options,
        headers
    });

    // Automatically log out if the server rejects an expired
    // or otherwise invalid token.
    if (response.status === 401 && token) {
        sessionStorage.removeItem(AUTH_STORAGE_KEY);

        window.dispatchEvent(
            new Event("stockflow:unauthorized")
        );
    }

    return response;
}

export { AUTH_STORAGE_KEY };
