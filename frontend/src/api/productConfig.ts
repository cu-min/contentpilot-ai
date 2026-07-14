import request, { ApiResult } from './request';
import type { ProductConfig } from '../types/product';

export function getProductConfig() {
  return request.get<unknown, ApiResult<ProductConfig>>('/product-config');
}

export function saveProductConfig(data: ProductConfig) {
  return request.put<unknown, ApiResult<ProductConfig>>('/product-config', data);
}

export function listProductConfigs() {
  return request.get<unknown, ApiResult<ProductConfig[]>>('/product-configs');
}

export function createProductConfig(data: ProductConfig) {
  return request.post<unknown, ApiResult<ProductConfig>>('/product-configs', data);
}

export function updateProductConfig(id: number, data: ProductConfig) {
  return request.put<unknown, ApiResult<ProductConfig>>(`/product-configs/${id}`, data);
}

export function deleteProductConfig(id: number) {
  return request.delete<unknown, ApiResult<null>>(`/product-configs/${id}`);
}
