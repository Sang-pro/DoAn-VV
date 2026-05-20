// Auth types
export interface LoginRequest {
  username: string;
  password: string;
}

export interface JwtResponse {
  accessToken: string;
  tokenType: string;
  username: string;
  id?: string;
  email?: string;
  roles?: string[];
}

export interface User {
  id: string;
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
  displayName?: string;
  phoneNumber?: string;
  isActive: boolean;
  lockedUntil?: string;
  roles: string[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateUser {
    username: string;
    email: string;
    password?: string;
    firstName?: string;
    lastName?: string;
    phoneNumber?: string;
    roles?: string[];
}

export interface UpdateUser {
    firstName?: string;
    lastName?: string;
    phoneNumber?: string;
    roles?: string[];
}

export interface UserStatusUpdate {
    isActive: boolean;
}

// MQTT types
export interface MqttCredentialsRequest {
  mqttUsername: string;
  mqttPassword: string;
  brokerUrl: string;
}

export interface MqttCredentialsResponse {
  id: string;
  mqttUsername: string;
  brokerUrl: string;
  isActive: boolean;
  createdAt: string;
  updatedAt?: string;
}

// Sensor data types
export interface SensorData {
  id: string;
  username: string;
  sensorType: string;
  sensorValue: number;
  unit: string;
  timestamp: string;
}

// Message response
export interface MessageResponse {
  message: string;
  success: boolean;
  token?: string;
}

// Pagination
export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  currentPage: number;
  pageSize: number;
}

// Book & ESL Types
export interface Book {
  id?: number;
  isbn: string;
  title: string;
  author: string;
  availableCopies: number;
  summary?: string;
  coverImageUrl?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface EslTag {
  id?: number;
  macAddress: string;
  batteryLevel?: number;
  isOnline?: boolean;
  location?: string;
  status?: string;
  book?: Book;
}
