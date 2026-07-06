import { Button, Form, Input, Modal, Popconfirm, Space, Table, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useState } from 'react';
import {
  createProductConfig,
  deleteProductConfig,
  listProductConfigs,
  updateProductConfig,
} from '../../api/productConfig';
import PageContainer from '../../components/PageContainer';
import SectionCard from '../../components/SectionCard';
import type { ProductConfig } from '../../types/product';
import { formatFailure } from '../../utils/feedback';

const { TextArea } = Input;

export default function Product() {
  const [form] = Form.useForm<ProductConfig>();
  const [products, setProducts] = useState<ProductConfig[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState<ProductConfig | null>(null);

  const loadProducts = async () => {
    setLoading(true);
    try {
      const result = await listProductConfigs();
      setProducts(result.data || []);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '产品配置加载失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadProducts();
  }, []);

  const openCreateModal = () => {
    setEditingProduct(null);
    form.resetFields();
    setModalOpen(true);
  };

  const openEditModal = (product: ProductConfig) => {
    setEditingProduct(product);
    form.setFieldsValue(product);
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    const values = await form.validateFields();
    setSaving(true);
    try {
      if (editingProduct?.id) {
        await updateProductConfig(editingProduct.id, values);
        message.success('产品配置已更新');
      } else {
        await createProductConfig(values);
        message.success('产品配置已新增');
      }
      setModalOpen(false);
      setEditingProduct(null);
      form.resetFields();
      await loadProducts();
    } catch (error) {
      message.error(formatFailure(editingProduct ? '更新' : '新增', error));
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await deleteProductConfig(id);
      message.success('产品配置已删除');
      await loadProducts();
    } catch (error) {
      message.error(formatFailure('删除', error));
    }
  };

  const columns: ColumnsType<ProductConfig> = [
    {
      title: '产品名称',
      dataIndex: 'productName',
      width: 180,
      render: (value: string) => <Typography.Text strong>{value || '-'}</Typography.Text>,
    },
    {
      title: '官网链接',
      dataIndex: 'officialUrl',
      width: 220,
      render: (value?: string) => value || '-',
    },
    {
      title: '目标用户',
      dataIndex: 'targetUsers',
      ellipsis: true,
      render: (value?: string) => value || '-',
    },
    {
      title: '品牌语气',
      dataIndex: 'brandTone',
      width: 160,
      render: (value?: string) => value || '-',
    },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      width: 180,
      render: (value?: string) => (value ? value.replace('T', ' ').slice(0, 16) : '-'),
    },
    {
      title: '操作',
      key: 'actions',
      width: 150,
      render: (_, record) => (
        <Space>
          <Button type="link" size="small" onClick={() => openEditModal(record)}>
            编辑
          </Button>
          <Popconfirm
            title="删除产品配置"
            description="删除后 AI 生成将无法再选择该产品。确定删除吗？"
            okText="删除"
            cancelText="取消"
            onConfirm={() => record.id && handleDelete(record.id)}
          >
            <Button type="link" size="small" danger>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <PageContainer
      title="产品配置"
      description="维护多个推广产品的信息，AI 生成文章时可选择其中一个产品作为上下文，也可以不选择产品。"
    >
      <SectionCard>
        <Space style={{ width: '100%', justifyContent: 'space-between', marginBottom: 16 }}>
          <Typography.Text strong>产品列表</Typography.Text>
          <Button type="primary" onClick={openCreateModal}>
            新增产品
          </Button>
        </Space>
        <Table
          rowKey={(record) => String(record.id)}
          columns={columns}
          dataSource={products}
          loading={loading}
          pagination={{ pageSize: 10 }}
        />
      </SectionCard>

      <Modal
        title={editingProduct ? '编辑产品配置' : '新增产品配置'}
        open={modalOpen}
        onOk={() => void handleSubmit()}
        onCancel={() => {
          setModalOpen(false);
          setEditingProduct(null);
          form.resetFields();
        }}
        confirmLoading={saving}
        destroyOnClose
      >
        <Form form={form} layout="vertical" requiredMark="optional">
          <Form.Item
            label="产品名称"
            name="productName"
            rules={[{ required: true, message: '请输入产品名称' }]}
          >
            <Input placeholder="请输入产品名称" maxLength={100} />
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
        </Form>
      </Modal>
    </PageContainer>
  );
}
