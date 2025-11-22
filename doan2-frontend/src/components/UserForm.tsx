import React from 'react';
import { useForm } from 'react-hook-form';
import { User, CreateUser, UpdateUser } from '../types';

interface UserFormProps {
    user?: User;
    onSubmit: (data: CreateUser | UpdateUser) => void;
    onCancel: () => void;
}

// This type represents the shape of the form data, combining fields from both create and update types.
type UserFormData = CreateUser & { passwordConfirmation?: string };


const UserForm: React.FC<UserFormProps> = ({ user, onSubmit, onCancel }) => {
    const { register, handleSubmit, formState: { errors } } = useForm<UserFormData>({
        defaultValues: user || {},
    });

    const handleFormSubmit = (data: UserFormData) => {
        if (user) {
            // When updating, we don't send username, email, or password.
            // react-hook-form doesn't include disabled fields, so we just pass the data.
            onSubmit(data);
        } 
    };

    return (
        <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full">
            <div className="relative top-20 mx-auto p-5 border w-96 shadow-lg rounded-md bg-white">
                <h3 className="text-lg font-medium text-gray-900">{user ? 'Edit User' : 'Add User'}</h3>
                <form onSubmit={handleSubmit(handleFormSubmit)} className="mt-4">
                    <div className="mb-4">
                        <label htmlFor="username" className="block text-gray-700 text-sm font-bold mb-2">Username</label>
                        <input
                            id="username"
                            {...register('username', { required: 'Username is required' })}
                            className={`shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline ${user ? 'bg-gray-200' : ''}`}
                            disabled={!!user}
                        />
                        {errors.username && <p className="text-red-500 text-xs italic">{errors.username.message}</p>}
                    </div>
                    <div className="mb-4">
                        <label htmlFor="email" className="block text-gray-700 text-sm font-bold mb-2">Email</label>
                        <input
                            id="email"
                            type="email"
                            {...register('email', { required: 'Email is required' })}
                            className={`shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline ${user ? 'bg-gray-200' : ''}`}
                            disabled={!!user}
                        />
                        {errors.email && <p className="text-red-500 text-xs italic">{errors.email.message}</p>}
                    </div>
                    {!user && (
                         <div className="mb-4">
                            <label htmlFor="password"  className="block text-gray-700 text-sm font-bold mb-2">Password</label>
                            <input
                                id="password"
                                type="password"
                                {...register('password', { required: 'Password is required' })}
                                className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
                            />
                            {errors.password && <p className="text-red-500 text-xs italic">{errors.password.message}</p>}
                        </div>
                    )}
                    <div className="flex items-center justify-end">
                        <button type="button" onClick={onCancel} className="bg-gray-500 hover:bg-gray-700 text-white font-bold py-2 px-4 rounded focus:outline-none focus:shadow-outline mr-2">
                            Cancel
                        </button>
                        <button type="submit" className="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded focus:outline-none focus:shadow-outline">
                            {user ? 'Save' : 'Create'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default UserForm;
