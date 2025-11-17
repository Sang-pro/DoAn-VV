import apiClient from './client';
import { MqttCredentialsRequest, MqttCredentialsResponse, MessageResponse } from '../types';

export const mqttAPI = {
  register: (data: MqttCredentialsRequest) =>
    apiClient.post<MqttCredentialsResponse>('/mqtt/register', data),

  getById: (id: string) =>
    apiClient.get<MqttCredentialsResponse>(`/mqtt/${id}`),

  getByUsername: (username: string) =>
    apiClient.get<MqttCredentialsResponse>(`/mqtt/username/${username}`),

  getActive: () =>
    apiClient.get<MqttCredentialsResponse[]>('/mqtt/active'),

  update: (id: string, updates: any) =>
    apiClient.patch<MqttCredentialsResponse>(`/mqtt/${id}`, updates),

  delete: (id: string) =>
    apiClient.delete<MessageResponse>(`/mqtt/${id}`),

  deactivate: (id: string) =>
    apiClient.post<MessageResponse>(`/mqtt/${id}/deactivate`, {}),

  publishMessage: (topic: string, message: string, qos?: number) =>
    apiClient.post<MessageResponse>('/mqtt/publish', null, {
      params: { topic, message, qos: qos || 1 },
    }),

  getBrokerStatus: () =>
    apiClient.get<any>('/mqtt/broker-status'),
};

// Export as MqttApi (camelCase) for compatibility
export const MqttApi = mqttAPI;
