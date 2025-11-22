import React, { useEffect, useState, useCallback } from 'react';
import { listUsers, createUser, updateUser, deleteUser, updateUserStatus } from '../api/admin';
import { User, CreateUser, UpdateUser } from '../types';
import UserForm from '../components/UserForm';
import ConfirmationDialog from '../components/ConfirmationDialog';

const UserManagement: React.FC = () => {
    const [users, setUsers] = useState<User[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [isUserFormOpen, setIsUserFormOpen] = useState(false);
    const [isConfirmOpen, setIsConfirmOpen] = useState(false);
    const [selectedUser, setSelectedUser] = useState<User | undefined>(undefined);
    const [userToDelete, setUserToDelete] = useState<string | null>(null);
    const [search, setSearch] = useState('');
    const [isActive, setIsActive] = useState(true);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);

    const fetchUsers = useCallback(async () => {
        try {
            setLoading(true);
            const response = await listUsers(search, isActive, page, 10);
            setUsers(response.content || []);
            setTotalPages(response.totalPages || 0);
            setLoading(false);
        } catch (err) {
            console.error('Failed to fetch users:', err);
            setError('Failed to fetch users.');
            setLoading(false);
        }
    }, [search, isActive, page]);

    useEffect(() => {
        fetchUsers();
    }, [fetchUsers]);

    const handleCreateUser = () => {
        setSelectedUser(undefined);
        setIsUserFormOpen(true);
    };

    const handleEditUser = (user: User) => {
        setSelectedUser(user);
        setIsUserFormOpen(true);
    };

    const handleDeleteUser = (userId: string) => {
        setUserToDelete(userId);
        setIsConfirmOpen(true);
    };

    const handleConfirmDelete = async () => {
        if (userToDelete) {
            await deleteUser(userToDelete);
            fetchUsers();
            setIsConfirmOpen(false);
            setUserToDelete(null);
        }
    };

    const handleUserFormSubmit = async (data: CreateUser | UpdateUser) => {
        if (selectedUser) {
            await updateUser(selectedUser.id, data as UpdateUser);
        } else {
            await createUser(data as CreateUser);
        }
        fetchUsers();
        setIsUserFormOpen(false);
        setSelectedUser(undefined);
    };

    const handleStatusChange = async (userId: string, currentStatus: boolean) => {
        await updateUserStatus(userId, { isActive: !currentStatus });
        fetchUsers();
    };

    const renderContent = () => {
        if (loading) {
            return <div>Loading...</div>;
        }

        if (error) {
            return <div>{error}</div>;
        }

        return (
            <>
                <div className="bg-white shadow-md rounded-lg p-4">
                    <table className="min-w-full divide-y divide-gray-200">
                        <thead className="bg-gray-50">
                            <tr>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Username</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Email</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Is Active</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Roles</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
                            </tr>
                        </thead>
                        <tbody className="bg-white divide-y divide-gray-200">
                            {users.map((user) => (
                                <tr key={user.id}>
                                    <td className="px-6 py-4 whitespace-nowrap">{user.username}</td>
                                    <td className="px-6 py-4 whitespace-nowrap">{user.email}</td>
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <button onClick={() => handleStatusChange(user.id, user.isActive)}>
                                            {user.isActive ? 'Yes' : 'No'}
                                        </button>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap">{user.roles.join(', ')}</td>
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <button onClick={() => handleEditUser(user)} className="text-indigo-600 hover:text-indigo-900 mr-2">Edit</button>
                                        <button onClick={() => handleDeleteUser(user.id)} className="text-red-600 hover:text-red-900">Delete</button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
                <div className="flex justify-between items-center mt-4">
                    <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}>Previous</button>
                    <span>Page {page + 1} of {totalPages}</span>
                    <button onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page === totalPages - 1}>Next</button>
                </div>
            </>
        );
    }

    return (
        <div className="p-6">
            <h1 className="text-2xl font-bold mb-4">User Management</h1>
            
            <div className="flex justify-between items-center mb-4">
                <div className="flex items-center">
                    <input
                        type="text"
                        placeholder="Search..."
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                        className="border rounded p-2 mr-2"
                    />
                    <select 
                        aria-label="Filter by status"
                        value={String(isActive)} 
                        onChange={(e) => setIsActive(e.target.value === 'true')} 
                        className="border rounded p-2"
                    >
                        <option value="true">Active</option>
                        <option value="false">Inactive</option>
                    </select>
                </div>
                <button onClick={handleCreateUser} className="bg-blue-500 text-white p-2 rounded">
                    Add User
                </button>
            </div>

            {renderContent()}

            {isUserFormOpen && (
                <UserForm
                    user={selectedUser}
                    onSubmit={handleUserFormSubmit}
                    onCancel={() => {
                        setIsUserFormOpen(false);
                        setSelectedUser(undefined);
                    }}
                />
            )}
            {isConfirmOpen && (
                <ConfirmationDialog
                    message="Are you sure you want to delete this user?"
                    onConfirm={handleConfirmDelete}
                    onCancel={() => setIsConfirmOpen(false)}
                />
            )}
        </div>
    );
};

export default UserManagement;
