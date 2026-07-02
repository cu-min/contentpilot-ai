import { Button, Form, Input, Space, message } from 'antd';
import { useEffect, useState } from 'react';
import { getProductConfig, saveProductConfig } from '../../api/productConfig';
import PageContainer from '../../components/PageContainer';
import SectionCard from '../../components/SectionCard';
import type { ProductConfig } from '../../types/product';

const { TextArea } = Input;

export default function Product() {
  const [form] = Form.useForm<ProductConfig>();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  const loadConfig = async () => {
    setLoading(true);
    try {
      const result = await getProductConfig();
      form.setFieldsValue(result.data);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '产品配置加载失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadConfig();
  }, []);

  const handleFinish = async (values: ProductConfig) => {
    setSaving(true);
    try {
      const result = await saveProductConfig(values);
      form.setFieldsValue(result.data);
      message.success('产品配置保存成功');
    } catch (error) {
      message.error(error instanceof Error ? error.message : '产品配置保存失败');
    } finally {
      setSaving(false);
    }
  };

  return (
    <PageContainer
      title="产品配置"
      description="配置当前推广产品的基础信息，作为后续 AI 文章生成的默认上下文。系统当前只维护一个产品配置。"
    >
      <SectionCard loading={loading}>
        <Form
          form={form}
          layout="vertical"
          onFinish={handleFinish}
          requiredMark="optional"
        >
          <Form.Item
            label="产品名称"
            name="productName"
            rules={[{ required: true, message: '请输入产品名称' }]}
          >
            <Input placeholder="请输入当前推广产品名称" maxLength={100} />
          </Form.Item>
          <Form.Item label="产品简介" name="productIntro">
            <TextArea rows={4} placeholder="简要描述产品定位、核心价值和适用场景" />
          </Form.Item>
          <Form.Item
            label="官网链接"
            name="officialUrl"
            rules={[{ type: 'url', message: '请输入有效的 URL，例如 https://example.com' }]}
          >
            <Input placeholder="https://example.com" maxLength={255} />
          </Form.Item>
          <Form.Item label="核心功能" name="coreFeatures">
            <TextArea rows={4} placeholder="例如：功能1、功能2、功能3" />
          </Form.Item>
          <Form.Item label="目标用户" name="targetUsers">
            <TextArea rows={3} placeholder="例如：研发团队、运营人员、企业管理者" />
          </Form.Item>
          <Form.Item label="产品优势" name="advantages">
            <TextArea rows={4} placeholder="描述产品相对竞品或传统流程的优势" />
          </Form.Item>
          <Form.Item label="品牌语气" name="brandTone">
            <Input placeholder="例如：专业、清晰、技术向" maxLength={255} />
          </Form.Item>
          <Form.Item label="禁用词/敏感词" name="bannedWords">
            <TextArea rows={3} placeholder="多个词可用逗号分隔，例如：绝对领先,100%保证" />
          </Form.Item>
          <Space>
            <Button type="primary" htmlType="submit" loading={saving}>
              保存配置
            </Button>
            <Button onClick={() => void loadConfig()}>重新加载</Button>
          </Space>
        </Form>
      </SectionCard>
    </PageContainer>
  );
}
