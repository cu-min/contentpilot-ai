import request, { ApiResult } from './request';
import type { ProductConfig } from '../types/product';

export function getProductConfig() {
  return request.get<unknown, ApiResult<ProductConfig>>('/product-config');
}

export function saveProductConfig(data: ProductConfig) {
  return request.put<unknown, ApiResult<ProductConfig>>('/product-config', data);
}
