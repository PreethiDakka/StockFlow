
import { useState } from "react";
import {
    Menu,
    X,
    PanelLeftClose,
    PanelLeftOpen,
    LayoutDashboard,
    Package,
    Warehouse,
    Boxes,
    ArrowLeftRight,
    ClipboardList,
    Truck,
    BarChart3,
    LogOut
} from "lucide-react";

import Dashboard from "./Dashboard";
import Products from "./Products";
import Suppliers from "./Suppliers";
import Warehouses from "./Warehouses";
import StockMovements from "./StockMovements";
import PurchaseOrders from "./PurchaseOrders";
import Inventory from "./Inventory";
import Reports from "./Reports";

import { AuthProvider, useAuth } from "./context/AuthContext";

import Login from "./pages/Login";
import Register from "./pages/Register";

import "./App.css";
import "./pages/Auth.css";

const menuItems = [
    { name: "Dashboard", icon: LayoutDashboard },
    { name: "Products", icon: Package },
    { name: "Warehouses", icon: Warehouse },
    { name: "Inventory", icon: Boxes },
    { name: "Stock Movements", icon: ArrowLeftRight },
    { name: "Purchase Orders", icon: ClipboardList },
    { name: "Suppliers", icon: Truck },
    { name: "Reports & Analytics", icon: BarChart3 }
];

function AppContent() {
    const { user, logout } = useAuth();

    const [activePage, setActivePage] = useState("Dashboard");
    const [authPage, setAuthPage] = useState("login");
    const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
    const [mobileSidebarOpen, setMobileSidebarOpen] = useState(false);

    if (!user) {
        return authPage === "register" ? (
            <Register onSwitch={setAuthPage} />
        ) : (
            <Login onSwitch={setAuthPage} />
        );
    }

    const renderPage = () => {
        switch (activePage) {
            case "Dashboard":
                return <Dashboard />;
            case "Products":
                return <Products />;
            case "Warehouses":
                return <Warehouses />;
            case "Inventory":
                return <Inventory />;
            case "Stock Movements":
                return <StockMovements />;
            case "Purchase Orders":
                return <PurchaseOrders />;
            case "Suppliers":
                return <Suppliers />;
            case "Reports & Analytics":
                return <Reports />;
            default:
                return <Dashboard />;
        }
    };

    const toggleSidebar = () => {
        if (window.matchMedia("(max-width: 768px)").matches) {
            setMobileSidebarOpen(previous => !previous);
        } else {
            setSidebarCollapsed(previous => !previous);
        }
    };

    const handlePageChange = (page) => {
        setActivePage(page);
        setMobileSidebarOpen(false);
    };

    const handleLogout = () => {
        logout();
        setActivePage("Dashboard");
        setAuthPage("login");
        setMobileSidebarOpen(false);
    };

    return (
        <div className={`app ${sidebarCollapsed ? "sidebar-collapsed" : ""}`}>
            {mobileSidebarOpen && (
                <button
                    className="sidebar-backdrop"
                    aria-label="Close navigation"
                    onClick={() => setMobileSidebarOpen(false)}
                />
            )}

            <aside
                className={`sidebar ${
                    mobileSidebarOpen ? "sidebar-mobile-open" : ""
                }`}
            >
                <div className="sidebar-brand">
                    <div className="brand-logo">S</div>
                    <h2 className="brand-name">StockFlow</h2>
                </div>

                <div className="sidebar-user">
                    <div className="user-avatar">
                        {user.username?.charAt(0).toUpperCase()}
                    </div>
                    <div className="welcome-text">
                        <span>Welcome back</span>
                        <strong>{user.username}</strong>
                    </div>
                </div>

                <p className="nav-heading">MAIN MENU</p>

                <nav>
                    {menuItems.map((item) => {
                        const Icon = item.icon;

                        return (
                            <button
                                key={item.name}
                                type="button"
                                title={sidebarCollapsed ? item.name : ""}
                                className={
                                    activePage === item.name ? "active" : ""
                                }
                                onClick={() => handlePageChange(item.name)}
                            >
                                <Icon
                                    className="nav-icon"
                                    size={20}
                                    strokeWidth={1.8}
                                />
                                <span className="nav-label">
                                    {item.name}
                                </span>
                            </button>
                        );
                    })}
                </nav>

                <div className="sidebar-bottom">
                    <button
                        type="button"
                        className="auth-logout-btn"
                        onClick={handleLogout}
                        title={sidebarCollapsed ? "Logout" : ""}
                    >
                        <LogOut
                            className="nav-icon"
                            size={20}
                        />
                        <span className="logout-label">Logout</span>
                    </button>
                </div>
            </aside>

            <main className="main">
                <div className="app-topbar">
                    <button
                        type="button"
                        className="sidebar-toggle"
                        onClick={toggleSidebar}
                        aria-label="Toggle sidebar"
                        title="Toggle sidebar"
                    >
                        {mobileSidebarOpen ? (
                            <X size={21} />
                        ) : sidebarCollapsed ? (
                            <PanelLeftOpen size={21} />
                        ) : window.matchMedia("(max-width: 768px)").matches ? (
                            <Menu size={21} />
                        ) : (
                            <PanelLeftClose size={21} />
                        )}
                    </button>

                    <div className="topbar-page-title">
                        {activePage}
                    </div>

                    <div className="topbar-user">
                        <div className="topbar-avatar">
                            {user.username?.charAt(0).toUpperCase()}
                        </div>
                        <span>{user.username}</span>
                    </div>
                </div>

                <div className="page-content">
                    {renderPage()}
                </div>
            </main>
        </div>
    );
}

function App() {
    return (
        <AuthProvider>
            <AppContent />
        </AuthProvider>
    );
}

export default App;
