import { Button, Form, Input, Modal, Space, Table, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useState } from 'react';
import { getProductConfig, saveProductConfig } from '../../api/productConfig';
import PageContainer from '../../components/PageContainer';
import SectionCard from '../../components/SectionCard';
import type { ProductConfig } from '../../types/product';
import { formatDateTime } from '../../utils/datetime';
import { formatFailure } from '../../utils/feedback';

const { TextArea } = Input;

export default function Product() {
  const [form] = Form.useForm<ProductConfig>();
  const [product, setProduct] = useState<ProductConfig | null>(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);

  const loadProduct = async () => {
    setLoading(true);
    try {
      const result = await getProductConfig();
      setProduct(result.data?.id ? result.data : null);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '产品配置加载失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadProduct();
  }, []);

  const openEditModal = () => {
    form.setFieldsValue(product || {});
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    const values = await form.validateFields();
    setSaving(true);
    try {
      await saveProductConfig(values);
      message.success('产品配置已保存');
      setModalOpen(false);
      form.resetFields();
      await loadProduct();
    } catch (error) {
      message.error(formatFailure('保存', error));
    } finally {
      setSaving(false);
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
      ellipsis: true,
      render: (value?: string) => value ? (
        <Typography.Link href={value} target="_blank" rel="noreferrer" ellipsis>
          {value}
        </Typography.Link>
      ) : '-',
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
      render: (value?: string) => formatDateTime(value),
    },
    {
      title: '操作',
      key: 'actions',
      width: 170,
      render: () => (
        <Button type="primary" size="small" onClick={openEditModal}>
          编辑
        </Button>
      ),
    },
  ];

  return (
    <PageContainer
      title="产品配置"
      description="把产品亮点、目标用户和品牌语气整理好，让每一次内容生成都更贴近你的产品。"
    >
      <SectionCard>
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <Space style={{ width: '100%', justifyContent: 'space-between' }}>
            <Typography.Text strong>当前产品配置</Typography.Text>
            <Button type="primary" onClick={openEditModal}>
              {product ? '编辑产品配置' : '配置产品'}
            </Button>
          </Space>
        <Table
          rowKey={(record) => String(record.id)}
          columns={columns}
          dataSource={product ? [product] : []}
          loading={loading}
          pagination={{ pageSize: 10 }}
        />
        </Space>
      </SectionCard>

      <Modal
        title={product ? '编辑产品配置' : '配置产品'}
        open={modalOpen}
        onOk={() => void handleSubmit()}
        onCancel={() => {
          setModalOpen(false);
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
