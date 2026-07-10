import {
  Button,
  Empty,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import type { TableColumnsType } from 'antd';
import { useEffect, useState } from 'react';
import {
  checkGrowthTrackingTarget,
  createGrowthTrackingTarget,
  deleteGrowthTrackingTarget,
  getGrowthTrackingTargets,
  updateGrowthTrackingTarget,
  updateGrowthTrackingTargetStatus,
} from '../../../api/growthTracking';
import PageContainer from '../../../components/PageContainer';
import SectionCard from '../../../components/SectionCard';
import type {
  GrowthCheckStatus,
  GrowthTrackingPlatform,
  GrowthTrackingTarget,
  GrowthTrackingTargetPayload,
  GrowthTrackingTargetQuery,
} from '../../../types/growthTracking';
import {
  getGrowthCheckStatusLabel,
  growthTrackingPlatformOptions,
} from '../../../types/growthTracking';
import { formatDateTime } from '../../../utils/datetime';
import { formatFailure } from '../../../utils/feedback';

interface GrowthTrackingFilterValues {
  name?: string;
  platform?: GrowthTrackingPlatform;
  enabled?: number;
}

export default function GrowthTracking() {
  const [form] = Form.useForm<GrowthTrackingTargetPayload>();
  const [filterForm] = Form.useForm<GrowthTrackingFilterValues>();
  const [targets, setTargets] = useState<GrowthTrackingTarget[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [checkingId, setCheckingId] = useState<number | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<GrowthTrackingTarget | null>(null);

  const loadTargets = async (filters?: GrowthTrackingTargetQuery) => {
    setLoading(true);
    try {
      const result = await getGrowthTrackingTargets(filters);
      setTargets(result.data);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '跟踪目标加载失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadTargets();
  }, []);

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({
      platform: 'WECHAT_OFFICIAL',
      enabled: 1,
    });
    setModalOpen(true);
  };

  const openEdit = (record: GrowthTrackingTarget) => {
    setEditing(record);
    form.setFieldsValue({
      name: record.name,
      platform: record.platform,
      targetUrl: record.targetUrl,
      remark: record.remark || undefined,
      enabled: record.enabled,
    });
    setModalOpen(true);
  };

  const handleSave = async () => {
    const values = await form.validateFields();
    setSaving(true);
    try {
      if (editing) {
        await updateGrowthTrackingTarget(editing.id, values);
      } else {
        await createGrowthTrackingTarget(values);
      }
      message.success('保存成功');
      setModalOpen(false);
      setEditing(null);
      form.resetFields();
      await loadTargets(filterForm.getFieldsValue());
    } catch (error) {
      message.error(formatFailure('保存', error));
    } finally {
      setSaving(false);
    }
  };

  const handleFilter = async () => {
    await loadTargets(filterForm.getFieldsValue());
  };

  const handleStatusChange = async (record: GrowthTrackingTarget, checked: boolean) => {
    try {
      await updateGrowthTrackingTargetStatus(record.id, checked ? 1 : 0);
      message.success(checked ? '启用成功' : '停用成功');
      await loadTargets(filterForm.getFieldsValue());
    } catch (error) {
      message.error(formatFailure(checked ? '启用' : '停用', error));
    }
  };

  const handleCheck = async (record: GrowthTrackingTarget) => {
    setCheckingId(record.id);
    try {
      await checkGrowthTrackingTarget(record.id);
      message.success('查询完成');
      await loadTargets(filterForm.getFieldsValue());
    } catch (error) {
      message.error(error instanceof Error ? error.message : '查询失败');
    } finally {
      setCheckingId(null);
    }
  };

  const handleDelete = async (record: GrowthTrackingTarget) => {
    try {
      await deleteGrowthTrackingTarget(record.id);
      message.success('删除成功');
      await loadTargets(filterForm.getFieldsValue());
    } catch (error) {
      message.error(formatFailure('删除', error));
    }
  };

  const renderCheckStatus = (status: GrowthCheckStatus) => (
    <Tag color={status === 'SUCCESS' ? 'success' : status === 'FAILED' ? 'error' : 'default'}>
      {getGrowthCheckStatusLabel(status)}
    </Tag>
  );

  const columns: TableColumnsType<GrowthTrackingTarget> = [
    {
      title: '跟踪名称',
      dataIndex: 'name',
      width: 160,
      ellipsis: true,
    },
    {
      title: '平台',
      dataIndex: 'platformLabel',
      width: 130,
      render: (value: string) => <Tag color="processing">{value}</Tag>,
    },
    {
      title: '目标链接',
      dataIndex: 'targetUrl',
      width: 280,
      ellipsis: true,
      render: (value: string) => (
        <Typography.Link href={value} target="_blank" rel="noreferrer">
          {value}
        </Typography.Link>
      ),
    },
    {
      title: '启用状态',
      dataIndex: 'enabled',
      width: 110,
      render: (value: number) => (
        <Tag color={value === 1 ? 'success' : 'default'}>{value === 1 ? '启用' : '停用'}</Tag>
      ),
    },
    {
      title: '最近查询状态',
      dataIndex: 'lastCheckStatus',
      width: 130,
      render: (value: GrowthCheckStatus) => renderCheckStatus(value),
    },
    {
      title: 'HTTP 状态码',
      dataIndex: 'lastHttpStatus',
      width: 120,
      render: (value?: number | null) => value || '-',
    },
    {
      title: '页面标题',
      dataIndex: 'lastPageTitle',
      width: 220,
      ellipsis: true,
      render: (value?: string | null) => value || '-',
    },
    {
      title: '最近查询时间',
      dataIndex: 'lastCheckedAt',
      width: 180,
      render: (value?: string | null) => formatDateTime(value),
    },
    {
      title: '操作',
      key: 'action',
      width: 270,
      render: (_, record) => (
        <Space size={8}>
          <Button
            size="small"
            loading={checkingId === record.id}
            disabled={record.enabled !== 1 || checkingId !== null}
            onClick={() => void handleCheck(record)}
          >
            查询
          </Button>
          <Button size="small" onClick={() => openEdit(record)}>编辑</Button>
          <Switch
            size="small"
            checked={record.enabled === 1}
            checkedChildren="启用"
            unCheckedChildren="停用"
            onChange={(checked) => void handleStatusChange(record, checked)}
          />
          <Popconfirm
            title="确认删除这个跟踪目标吗？"
            okText="删除"
            cancelText="取消"
            onConfirm={() => void handleDelete(record)}
          >
            <Button size="small" danger>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <PageContainer
      title="跟踪查询"
      description="跟踪查询用于记录外部文章或推广链接，并手动检查链接当前是否可访问。当前仅做单次查询和结果记录。"
    >
      <SectionCard>
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <Space style={{ width: '100%', justifyContent: 'space-between' }}>
            <Typography.Text strong>跟踪目标列表</Typography.Text>
            <Button type="primary" onClick={openCreate}>新增跟踪目标</Button>
          </Space>
          <Form form={filterForm} layout="inline" onFinish={() => void handleFilter()}>
            <Form.Item name="name">
              <Input placeholder="搜索跟踪名称" allowClear />
            </Form.Item>
            <Form.Item name="platform">
              <Select
                allowClear
                placeholder="选择平台"
                style={{ width: 150 }}
                options={[...growthTrackingPlatformOptions]}
              />
            </Form.Item>
            <Form.Item name="enabled">
              <Select
                allowClear
                placeholder="启用状态"
                style={{ width: 130 }}
                options={[
                  { label: '启用', value: 1 },
                  { label: '停用', value: 0 },
                ]}
              />
            </Form.Item>
            <Form.Item>
              <Button htmlType="submit">查询</Button>
            </Form.Item>
          </Form>
          <Table
            rowKey="id"
            loading={loading}
            columns={columns}
            dataSource={targets}
            pagination={false}
            scroll={{ x: 1600 }}
            locale={{
              emptyText: (
                <Empty description="暂无跟踪目标配置">
                  <Button type="primary" onClick={openCreate}>新增跟踪目标</Button>
                </Empty>
              ),
            }}
          />
        </Space>
      </SectionCard>

      <Modal
        title={editing ? '编辑跟踪目标' : '新增跟踪目标'}
        open={modalOpen}
        confirmLoading={saving}
        onCancel={() => setModalOpen(false)}
        onOk={() => void handleSave()}
        okText="保存"
        cancelText="取消"
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item
            label="跟踪名称"
            name="name"
            rules={[{ required: true, message: '请输入跟踪名称' }]}
          >
            <Input placeholder="例如：官网文章推广链接" />
          </Form.Item>
          <Form.Item
            label="平台"
            name="platform"
            rules={[{ required: true, message: '请选择平台' }]}
          >
            <Select options={[...growthTrackingPlatformOptions]} />
          </Form.Item>
          <Form.Item
            label="目标链接"
            name="targetUrl"
            rules={[
              { required: true, message: '请输入目标链接' },
              { type: 'url', message: '请输入合法的 URL' },
            ]}
          >
            <Input placeholder="https://example.com/article" />
          </Form.Item>
          <Form.Item label="备注" name="remark">
            <Input.TextArea rows={3} placeholder="补充渠道或文章说明" />
          </Form.Item>
          <Form.Item
            label="启用"
            name="enabled"
            valuePropName="checked"
            getValueFromEvent={(checked: boolean) => (checked ? 1 : 0)}
            getValueProps={(value?: number) => ({ checked: value === 1 })}
          >
            <Switch checkedChildren="启用" unCheckedChildren="停用" />
          </Form.Item>
        </Form>
      </Modal>
    </PageContainer>
  );
}
