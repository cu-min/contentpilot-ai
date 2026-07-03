import {
  Alert,
  Button,
  DatePicker,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import type { TableColumnsType } from 'antd';
import dayjs from 'dayjs';
import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { getArticles } from '../../api/article';
import { getPlatformAccounts } from '../../api/platformAccount';
import { getArticlePlatformContents, getPlatformContentDetail } from '../../api/platformContent';
import {
  cancelPublishTask,
  createPublishTask,
  executePublishTask,
  getPublishTaskDetail,
  getPublishTasks,
  submitPublishTask,
  updatePublishTask,
} from '../../api/publishTask';
import PageContainer from '../../components/PageContainer';
import SectionCard from '../../components/SectionCard';
import type { ArticleListItem } from '../../types/article';
import type { PlatformAccount } from '../../types/platformAccount';
import { getPublishModeLabel } from '../../types/platformAccount';
import type { ArticlePlatformContent, PlatformContentPlatform } from '../../types/platformContent';
import { getPlatformContentPlatformLabel, platformContentOptions } from '../../types/platformContent';
import type { PublishTask, PublishTaskPayload, PublishTaskStatus, PublishType } from '../../types/publishTask';
import {
  getPublishTaskStatusLabel,
  getPublishTypeLabel,
  publishTaskStatusOptions,
  publishTypeOptions,
} from '../../types/publishTask';

interface PublishTaskFormValues {
  articleId?: number;
  platformContentId: number;
  accountId: number;
  title?: string;
  publishType: PublishType;
  scheduleTime?: unknown;
}

export default function Publish() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [form] = Form.useForm<PublishTaskFormValues>();
  const [filterForm] = Form.useForm();
  const [tasks, setTasks] = useState<PublishTask[]>([]);
  const [articles, setArticles] = useState<ArticleListItem[]>([]);
  const [platformContents, setPlatformContents] = useState<ArticlePlatformContent[]>([]);
  const [accounts, setAccounts] = useState<PlatformAccount[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [executingTaskId, setExecutingTaskId] = useState<number | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<PublishTask | null>(null);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });

  const selectedArticleId = Form.useWatch('articleId', form);
  const selectedPlatformContentId = Form.useWatch('platformContentId', form);
  const selectedPublishType = Form.useWatch('publishType', form);

  const selectedPlatformContent = useMemo(
    () => platformContents.find((item) => item.id === selectedPlatformContentId),
    [platformContents, selectedPlatformContentId],
  );

  const loadTasks = async (
    page = pagination.current,
    size = pagination.pageSize,
  ) => {
    setLoading(true);
    try {
      const filters = filterForm.getFieldsValue();
      const result = await getPublishTasks({
        page,
        size,
        platform: filters.platform,
        status: filters.status,
        publishType: filters.publishType,
      });
      setTasks(result.data.records);
      setPagination({
        current: result.data.current,
        pageSize: result.data.size,
        total: result.data.total,
      });
    } catch (error) {
      message.error(error instanceof Error ? error.message : '发布任务加载失败');
    } finally {
      setLoading(false);
    }
  };

  const loadArticles = async () => {
    try {
      const result = await getArticles({ page: 1, size: 100 });
      setArticles(result.data.records);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '文章列表加载失败');
    }
  };

  const loadAccounts = async (platform?: PlatformContentPlatform) => {
    try {
      const result = await getPlatformAccounts(platform ? { platform } : undefined);
      setAccounts(result.data);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '平台账号加载失败');
    }
  };

  const loadPlatformContents = async (articleId?: number) => {
    if (!articleId) {
      setPlatformContents([]);
      return;
    }
    try {
      const result = await getArticlePlatformContents(articleId);
      setPlatformContents(result.data);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '平台稿加载失败');
    }
  };

  useEffect(() => {
    void loadTasks(1, pagination.pageSize);
    void loadArticles();
    void loadAccounts();
  }, []);

  useEffect(() => {
    if (!selectedArticleId) {
      setPlatformContents([]);
      return;
    }
    void loadPlatformContents(selectedArticleId);
  }, [selectedArticleId]);

  useEffect(() => {
    if (!selectedPlatformContent) {
      return;
    }
    form.setFieldValue('title', selectedPlatformContent.title);
    void loadAccounts(selectedPlatformContent.platform);
  }, [selectedPlatformContent, form]);

  useEffect(() => {
    const platformContentId = Number(searchParams.get('platformContentId'));
    if (!platformContentId) {
      return;
    }
    void openCreate(platformContentId);
    setSearchParams({});
  }, [searchParams, setSearchParams]);

  const openCreate = async (platformContentId?: number) => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({ publishType: 'IMMEDIATE' });
    setModalOpen(true);
    if (!platformContentId) {
      await loadAccounts();
      return;
    }
    try {
      const result = await getPlatformContentDetail(platformContentId);
      const content = result.data;
      form.setFieldsValue({
        articleId: content.articleId,
        platformContentId: content.id,
        title: content.title,
        publishType: 'IMMEDIATE',
      });
      await loadPlatformContents(content.articleId);
      await loadAccounts(content.platform);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '平台稿详情加载失败');
    }
  };

  const openEdit = async (record: PublishTask) => {
    try {
      const result = await getPublishTaskDetail(record.id);
      const task = result.data;
      setEditing(task);
      await loadPlatformContents(task.articleId);
      await loadAccounts(task.platform);
      form.setFieldsValue({
        articleId: task.articleId,
        platformContentId: task.platformContentId,
        accountId: task.accountId,
        title: task.title,
        publishType: task.publishType,
        scheduleTime: task.scheduleTime ? dayjs(task.scheduleTime) : undefined,
      });
      setModalOpen(true);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '发布任务详情加载失败');
    }
  };

  const normalizePayload = (values: PublishTaskFormValues): PublishTaskPayload => {
    const scheduleTimeValue = values.scheduleTime as { format?: (format: string) => string } | undefined;
    return {
      platformContentId: values.platformContentId,
      accountId: values.accountId,
      title: values.title,
      publishType: values.publishType,
      scheduleTime: values.publishType === 'SCHEDULED' && scheduleTimeValue?.format
        ? scheduleTimeValue.format('YYYY-MM-DDTHH:mm:ss')
        : undefined,
    };
  };

  const handleSave = async () => {
    const values = await form.validateFields();
    setSaving(true);
    try {
      if (editing) {
        await updatePublishTask(editing.id, normalizePayload(values));
        message.success('发布任务已更新');
      } else {
        await createPublishTask(normalizePayload(values));
        message.success('发布任务草稿已创建');
      }
      setModalOpen(false);
      setEditing(null);
      form.resetFields();
      await loadTasks(1, pagination.pageSize);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '发布任务保存失败');
    } finally {
      setSaving(false);
    }
  };

  const handleSubmit = async (record: PublishTask) => {
    try {
      await submitPublishTask(record.id);
      message.success('发布任务已提交为待发布');
      await loadTasks();
    } catch (error) {
      message.error(error instanceof Error ? error.message : '提交失败');
    }
  };

  const handleCancel = async (record: PublishTask) => {
    try {
      await cancelPublishTask(record.id);
      message.success('发布任务已取消');
      await loadTasks();
    } catch (error) {
      message.error(error instanceof Error ? error.message : '取消失败');
    }
  };

  const handleExecute = async (record: PublishTask) => {
    setExecutingTaskId(record.id);
    try {
      const result = await executePublishTask(record.id);
      if (result.data.status === 'SUCCESS') {
        message.success('MockPublisher 模拟发布成功');
      } else {
        message.error(result.data.errorMessage || 'MockPublisher 模拟发布失败');
      }
      await loadTasks();
    } catch (error) {
      message.error(error instanceof Error ? error.message : '执行失败');
    } finally {
      setExecutingTaskId(null);
    }
  };

  const statusColor = (status: PublishTaskStatus) => {
    if (status === 'DRAFT') return 'default';
    if (status === 'PENDING') return 'processing';
    if (status === 'RUNNING') return 'blue';
    if (status === 'SUCCESS') return 'success';
    if (status === 'FAILED') return 'error';
    if (status === 'CANCELLED') return 'warning';
    return 'default';
  };

  const columns: TableColumnsType<PublishTask> = [
    {
      title: '任务标题',
      dataIndex: 'title',
      ellipsis: true,
    },
    {
      title: '平台',
      dataIndex: 'platform',
      width: 130,
      render: (platform: PlatformContentPlatform) => (
        <Tag color="processing">{getPlatformContentPlatformLabel(platform)}</Tag>
      ),
    },
    {
      title: '账号',
      dataIndex: 'accountName',
      width: 150,
      ellipsis: true,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 110,
      render: (status: PublishTaskStatus) => (
        <Tag color={statusColor(status)}>{getPublishTaskStatusLabel(status)}</Tag>
      ),
    },
    {
      title: '发布类型',
      dataIndex: 'publishType',
      width: 120,
      render: (type: PublishType) => <Tag>{getPublishTypeLabel(type)}</Tag>,
    },
    {
      title: '发布方式',
      dataIndex: 'publishMode',
      width: 150,
      render: (mode: string) => <Tag>{getPublishModeLabel(mode)}</Tag>,
    },
    {
      title: '计划时间',
      dataIndex: 'scheduleTime',
      width: 180,
      responsive: ['md'],
      render: (value?: string) => value || '-',
    },
    {
      title: '发布结果',
      key: 'publishResult',
      width: 210,
      responsive: ['lg'],
      render: (_, record) => {
        if (record.status === 'SUCCESS' && record.publishUrl) {
          return (
            <Typography.Link href={record.publishUrl} target="_blank" rel="noreferrer">
              Mock 链接
            </Typography.Link>
          );
        }
        if (record.status === 'FAILED') {
          return (
            <Typography.Text type="danger" ellipsis={{ tooltip: record.errorMessage }}>
              {record.errorMessage || '执行失败'}
            </Typography.Text>
          );
        }
        return '-';
      },
    },
    {
      title: '操作',
      key: 'action',
      width: 300,
      render: (_, record) => (
        <Space size={8}>
          <Button size="small" disabled={record.status !== 'DRAFT'} onClick={() => openEdit(record)}>编辑</Button>
          <Button size="small" type="primary" disabled={record.status !== 'DRAFT'} onClick={() => handleSubmit(record)}>提交</Button>
          {record.status === 'PENDING' ? (
            <Popconfirm title="确认执行该发布任务？" okText="执行" cancelText="取消" onConfirm={() => handleExecute(record)}>
              <Button size="small" loading={executingTaskId === record.id}>执行任务</Button>
            </Popconfirm>
          ) : null}
          <Popconfirm title="确认取消该发布任务？" okText="取消任务" cancelText="返回" onConfirm={() => handleCancel(record)}>
            <Button size="small" disabled={!['DRAFT', 'PENDING'].includes(record.status)}>取消</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <PageContainer
      title="发布任务"
      description="创建平台发布稿的发布任务草稿，手动执行待发布任务，并用 MockPublisher 验证内部执行链路。"
    >
      <SectionCard>
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <Alert
            type="info"
            showIcon
            message="本阶段执行按钮只调用 MockPublisher，不会访问任何真实平台。"
            description="PENDING 任务可以手动执行；成功会写入 mock 链接，标题、摘要、正文、标签、关键词或账号备注包含 mock-fail 时会模拟失败。"
          />
          <Form form={filterForm} layout="inline" onFinish={() => loadTasks(1, pagination.pageSize)}>
            <Form.Item name="platform">
              <Select
                allowClear
                placeholder="平台"
                style={{ width: 150 }}
                options={[...platformContentOptions]}
              />
            </Form.Item>
            <Form.Item name="status">
              <Select
                allowClear
                placeholder="状态"
                style={{ width: 130 }}
                options={[...publishTaskStatusOptions]}
              />
            </Form.Item>
            <Form.Item name="publishType">
              <Select
                allowClear
                placeholder="发布类型"
                style={{ width: 140 }}
                options={[...publishTypeOptions]}
              />
            </Form.Item>
            <Form.Item>
              <Space>
                <Button htmlType="submit">筛选</Button>
                <Button onClick={() => {
                  filterForm.resetFields();
                  void loadTasks(1, pagination.pageSize);
                }}>重置</Button>
              </Space>
            </Form.Item>
          </Form>
          <Space style={{ width: '100%', justifyContent: 'space-between' }}>
            <Typography.Text strong>发布任务列表</Typography.Text>
            <Button type="primary" onClick={() => openCreate()}>创建发布任务</Button>
          </Space>
          <Table
            rowKey="id"
            loading={loading}
            columns={columns}
            dataSource={tasks}
            pagination={{
              current: pagination.current,
              pageSize: pagination.pageSize,
              total: pagination.total,
              onChange: (page, size) => loadTasks(page, size),
            }}
          />
        </Space>
      </SectionCard>

      <Modal
        title={editing ? '编辑发布任务草稿' : '创建发布任务草稿'}
        open={modalOpen}
        onCancel={() => {
          setModalOpen(false);
          setEditing(null);
        }}
        onOk={handleSave}
        okText="保存为草稿"
        cancelText="取消"
        confirmLoading={saving}
        width={760}
      >
        <Form form={form} layout="vertical" requiredMark="optional">
          <Form.Item
            label="文章"
            name="articleId"
            rules={[{ required: true, message: '请选择文章' }]}
          >
            <Select
              showSearch
              placeholder="请选择文章"
              optionFilterProp="label"
              options={articles.map((article) => ({ label: article.title, value: article.id }))}
            />
          </Form.Item>
          <Form.Item
            label="平台发布稿"
            name="platformContentId"
            rules={[{ required: true, message: '请选择平台发布稿' }]}
          >
            <Select
              placeholder="请选择平台发布稿"
              options={platformContents.map((content) => ({
                label: `${getPlatformContentPlatformLabel(content.platform)} - ${content.title}`,
                value: content.id,
              }))}
            />
          </Form.Item>
          <Form.Item
            label="平台账号"
            name="accountId"
            rules={[{ required: true, message: '请选择平台账号' }]}
            extra={selectedPlatformContent ? '只显示与当前平台稿同平台的账号。' : '请先选择平台发布稿。'}
          >
            <Select
              placeholder="请选择平台账号"
              options={accounts
                .filter((account) => !selectedPlatformContent || account.platform === selectedPlatformContent.platform)
                .map((account) => ({
                  label: `${account.accountName}${account.enabled === 1 ? '' : '（已禁用）'}`,
                  value: account.id,
                }))}
            />
          </Form.Item>
          <Form.Item label="任务标题" name="title">
            <Input maxLength={200} placeholder="默认使用平台稿标题" />
          </Form.Item>
          <Form.Item
            label="发布类型"
            name="publishType"
            rules={[{ required: true, message: '请选择发布类型' }]}
          >
            <Select options={[...publishTypeOptions]} />
          </Form.Item>
          {selectedPublishType === 'SCHEDULED' ? (
            <Form.Item
              label="计划发布时间"
              name="scheduleTime"
              rules={[{ required: true, message: '请选择计划发布时间' }]}
            >
              <DatePicker showTime style={{ width: '100%' }} />
            </Form.Item>
          ) : null}
        </Form>
      </Modal>
    </PageContainer>
  );
}
