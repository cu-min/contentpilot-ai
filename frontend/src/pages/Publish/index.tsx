import {
  Alert,
  Button,
  DatePicker,
  Empty,
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
import { useNavigate, useSearchParams } from 'react-router-dom';
import { getArticles } from '../../api/article';
import { getPlatformAccounts } from '../../api/platformAccount';
import { getArticlePlatformContents, getPlatformContentDetail } from '../../api/platformContent';
import {
  cancelPublishTask,
  createPublishTask,
  executePublishTask,
  getPublishTaskDetail,
  getPublishTasks,
  refreshPublishTaskStatus,
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
import { formatFailure, getErrorText } from '../../utils/feedback';

const CSDN_MANAGE_URL = 'https://mp.csdn.net/mp_blog/manage/article';

interface PublishTaskFormValues {
  articleId?: number;
  platformContentId: number;
  accountId: number;
  title?: string;
  publishType: PublishType;
  scheduleTime?: unknown;
}

export default function Publish() {
  const navigate = useNavigate();
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
  const [refreshing, setRefreshing] = useState(false);
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
      return true;
    } catch (error) {
      message.error(error instanceof Error ? error.message : '发布任务加载失败');
      return false;
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
        message.success('保存成功');
      } else {
        await createPublishTask(normalizePayload(values));
        message.success('保存成功');
      }
      setModalOpen(false);
      setEditing(null);
      form.resetFields();
      await loadTasks(1, pagination.pageSize);
    } catch (error) {
      message.error(formatFailure('保存', error));
    } finally {
      setSaving(false);
    }
  };

  const handleSubmit = async (record: PublishTask) => {
    try {
      await submitPublishTask(record.id);
      message.success('任务提交成功');
      await loadTasks();
    } catch (error) {
      message.error(formatFailure('任务提交', error));
    }
  };

  const handleCancel = async (record: PublishTask) => {
    try {
      await cancelPublishTask(record.id);
      message.success('取消成功');
      await loadTasks();
    } catch (error) {
      message.error(formatFailure('取消', error));
    }
  };

  const handleExecute = async (record: PublishTask) => {
    setExecutingTaskId(record.id);
    try {
      const result = await executePublishTask(record.id);
      if (result.data.status === 'SUCCESS') {
        message.success(record.platform === 'WECHAT_OFFICIAL' ? '公众号草稿创建成功' : '自动发布成功');
      } else if (result.data.status === 'RUNNING' && result.data.platform === 'WECHAT_OFFICIAL' && result.data.externalPublishId) {
        message.info('已提交微信发布，等待平台确认');
      } else if (result.data.status === 'NEED_MANUAL_CONFIRM') {
        message.info(result.data.errorMessage || '已自动填充，请在 Chrome for Testing 中检查并手动发布。');
      } else if (result.data.status === 'NEED_LOGIN') {
        message.warning(result.data.errorMessage || '需要登录后重新执行发布');
      } else if (result.data.status === 'NEED_CAPTCHA') {
        message.warning(result.data.errorMessage || '需要人工完成验证码后重新执行发布');
      } else {
        message.error(`自动发布失败：${result.data.errorMessage || getPublishTaskStatusLabel(result.data.status) || '发布失败'}`);
      }
      await loadTasks();
    } catch (error) {
      message.error(`自动发布失败：${getErrorText(error, '发布失败')}`);
    } finally {
      setExecutingTaskId(null);
    }
  };

  const handleRefreshStatus = async () => {
    setRefreshing(true);
    try {
      const refreshableTasks = tasks.filter((task) => (
        (
          task.platform === 'WECHAT_OFFICIAL'
          && task.status === 'RUNNING'
          && Boolean(task.externalPublishId)
        )
        || (
          task.platform === 'JUEJIN'
          && task.status === 'SUCCESS'
          && Boolean(task.externalArticleId || task.publishUrl || task.externalDraftId || task.draftUrl)
        )
      ));
      if (refreshableTasks.length > 0) {
        const results = await Promise.allSettled(
          refreshableTasks.map((task) => refreshPublishTaskStatus(task.id)),
        );
        const failedCount = results.filter((result) => result.status === 'rejected').length;
        const fulfilled = results
          .filter((result): result is PromiseFulfilledResult<Awaited<ReturnType<typeof refreshPublishTaskStatus>>> => result.status === 'fulfilled')
          .map((result) => result.value.data);
        const wechatSuccessCount = fulfilled.filter((task) => task.platform === 'WECHAT_OFFICIAL' && task.status === 'SUCCESS').length;
        const wechatProcessingCount = fulfilled.filter((task) => task.platform === 'WECHAT_OFFICIAL' && task.status === 'RUNNING').length;
        const wechatFailedCount = fulfilled.filter((task) => task.platform === 'WECHAT_OFFICIAL' && isFailureStatus(task.status)).length;
        if (wechatSuccessCount > 0) {
          message.success(`${wechatSuccessCount} 条微信公众号发布成功`);
        }
        if (wechatProcessingCount > 0) {
          message.info('微信发布仍在处理中，请稍后刷新');
        }
        if (wechatFailedCount > 0) {
          message.error(`${wechatFailedCount} 条微信公众号发布失败`);
        }
        if (failedCount > 0) {
          message.warning(`${failedCount} 条任务状态刷新失败，请稍后重试`);
        }
      } else {
        message.info('当前列表没有需要同步的平台状态');
      }
      const success = await loadTasks(pagination.current, pagination.pageSize);
      if (success && refreshableTasks.length === 0) {
        message.success('状态已刷新');
      }
    } finally {
      setRefreshing(false);
    }
  };

  const isFailureStatus = (status: PublishTaskStatus) => (
    [
      'FAILED',
      'NEED_LOGIN',
      'NEED_CAPTCHA',
      'LINK_FETCH_FAILED',
      'CONTENT_REJECTED',
    ] as PublishTaskStatus[]
  ).includes(status);

  const isManualConfirmStatus = (status: PublishTaskStatus) => status === 'NEED_MANUAL_CONFIRM';

  const statusColor = (status: PublishTaskStatus) => {
    if (status === 'DRAFT') return 'default';
    if (status === 'PENDING') return 'processing';
    if (status === 'RUNNING') return 'blue';
    if (status === 'SUCCESS') return 'success';
    if (isManualConfirmStatus(status)) return 'warning';
    if (isFailureStatus(status)) return 'error';
    if (status === 'CANCELLED') return 'warning';
    return 'default';
  };

  const articleStatus = (record: PublishTask) => {
    if (record.status === 'DRAFT' || record.status === 'PENDING') return null;
    if (record.status === 'RUNNING' && record.platform === 'WECHAT_OFFICIAL' && record.externalPublishId) {
      return { label: '微信确认中', color: 'blue' };
    }
    if (record.status === 'RUNNING' && record.platform === 'CSDN') {
      return { label: 'CSDN 自动化中', color: 'blue' };
    }
    if (record.status === 'RUNNING') return { label: '发布中', color: 'blue' };
    if (record.articleStatus === 'PUBLISHED') return { label: '已发布', color: 'success' };
    if (record.articleStatus === 'REJECTED') return { label: '未通过', color: 'error' };
    if (record.articleStatus === 'FAILED') return { label: '发布失败', color: 'error' };
    if (record.articleStatus === 'CANCELLED') return { label: '已取消', color: 'default' };
    if (record.articleStatus === 'SUBMITTED') return { label: '已提交，待取链', color: 'warning' };
    if (record.articleStatus === 'REVIEWING') {
      return { label: '审核中', color: 'warning' };
    }
    if (record.status === 'SUCCESS' && (isOfficialArticleUrl(record.publishUrl) || record.externalArticleId)) {
      return { label: '审核中', color: 'warning' };
    }
    if (record.status === 'SUCCESS') return { label: '已提交，待取链', color: 'warning' };
    if (record.status === 'CONTENT_REJECTED') return { label: '未通过', color: 'error' };
    if (record.status === 'NEED_LOGIN') return { label: '需登录', color: 'error' };
    if (record.status === 'NEED_CAPTCHA') return { label: '需验证码', color: 'error' };
    if (record.status === 'NEED_MANUAL_CONFIRM') return { label: '待人工确认', color: 'warning' };
    if (record.status === 'CANCELLED') return { label: '已取消', color: 'default' };
    if (isFailureStatus(record.status)) return { label: '发布失败', color: 'error' };
    return { label: getPublishTaskStatusLabel(record.status), color: statusColor(record.status) };
  };

  const isJuejinArticleUrl = (url?: string) => Boolean(url && url.includes('/post/'));

  const isCsdnArticleUrl = (url?: string) => Boolean(
    url
    && url.includes('csdn.net')
    && (url.includes('/article/details/') || url.includes('blog.csdn.net')),
  );

  const isOfficialArticleUrl = (url?: string) => isJuejinArticleUrl(url) || isCsdnArticleUrl(url);

  const isCsdnManageUrl = (url?: string) => Boolean(url?.includes('mp.csdn.net/mp_blog/manage/article'));

  const isCsdnEditorUrl = (url?: string) => Boolean(url?.includes('editor.csdn.net/md'));

  const isWechatDraftUrl = (url?: string) => Boolean(url?.startsWith('wechat-draft:'));

  const isWechatPublishUrl = (url?: string) => Boolean(url?.startsWith('wechat-publish:'));

  const handleViewResult = (record: PublishTask) => {
    if (record.platform === 'CSDN') {
      const url = record.publishUrl && !isCsdnEditorUrl(record.publishUrl)
        ? record.publishUrl
        : CSDN_MANAGE_URL;
      window.open(url || CSDN_MANAGE_URL, '_blank', 'noreferrer');
      return;
    }
    const url = record.publishUrl || record.draftUrl;
    if (!url || isWechatDraftUrl(url) || isWechatPublishUrl(url)) {
      if (isWechatPublishUrl(url)) {
        message.info(`微信 publish_id：${url?.replace('wechat-publish:', '') || ''}`);
        return;
      }
      message.info(url ? `微信公众号草稿 media_id：${url.replace('wechat-draft:', '')}` : '暂无结果链接');
      return;
    }
    window.open(url, '_blank', 'noreferrer');
  };

  const handleViewFailure = (record: PublishTask) => {
    Modal.error({
      title: '失败原因',
      content: record.errorMessage || getPublishTaskStatusLabel(record.status) || '暂无失败原因',
    });
  };

  const handleViewManualConfirm = (record: PublishTask) => {
    Modal.info({
      title: '人工确认',
      content: record.errorMessage || '已自动填充，请在 Chrome for Testing 中检查并手动发布。',
    });
  };

  const getPublishDisplayTime = (record: PublishTask) => {
    if (record.publishType === 'SCHEDULED' && record.scheduleTime) {
      return record.scheduleTime;
    }
    return record.updatedAt || record.createdAt || '-';
  };

  const renderTaskActions = (record: PublishTask) => {
    if (record.status === 'DRAFT') {
      return (
        <Space size={8} wrap>
          <Button size="small" onClick={() => openEdit(record)}>编辑</Button>
          <Button size="small" type="primary" onClick={() => handleSubmit(record)}>提交</Button>
          <Popconfirm title="确认取消该发布任务？" okText="取消任务" cancelText="返回" onConfirm={() => handleCancel(record)}>
            <Button size="small">取消</Button>
          </Popconfirm>
        </Space>
      );
    }

    if (record.status === 'PENDING') {
      return (
        <Space size={8} wrap>
          <Popconfirm title="确认自动发布该文章？" okText="自动发布" cancelText="取消" onConfirm={() => handleExecute(record)}>
            <Button size="small" type="primary" loading={executingTaskId === record.id}>自动发布</Button>
          </Popconfirm>
          <Popconfirm title="确认取消该发布任务？" okText="取消任务" cancelText="返回" onConfirm={() => handleCancel(record)}>
            <Button size="small">取消</Button>
          </Popconfirm>
        </Space>
      );
    }

    if (record.status === 'RUNNING') {
      return <Button size="small" disabled>{record.platform === 'CSDN' ? 'CSDN 自动化中' : '发布中'}</Button>;
    }

    if (record.status === 'SUCCESS') {
      return <Button size="small" type={record.platform === 'CSDN' ? 'primary' : 'default'} onClick={() => handleViewResult(record)}>查看文章</Button>;
    }

    if (record.status === 'NEED_MANUAL_CONFIRM') {
      if (record.platform === 'CSDN') {
        return <Button size="small" type="primary" onClick={() => handleViewResult(record)}>查看文章</Button>;
      }
      return <Button size="small" onClick={() => handleViewManualConfirm(record)}>查看提示</Button>;
    }

    if (isFailureStatus(record.status)) {
      return <Button size="small" danger onClick={() => handleViewFailure(record)}>查看失败原因</Button>;
    }

    return <Typography.Text type="secondary">不可操作</Typography.Text>;
  };

  const columns: TableColumnsType<PublishTask> = [
    {
      title: '任务标题',
      dataIndex: 'title',
      width: 220,
      ellipsis: true,
    },
    {
      title: '平台',
      dataIndex: 'platform',
      width: 110,
      render: (platform: PlatformContentPlatform) => (
        <Tag color="processing">{getPlatformContentPlatformLabel(platform)}</Tag>
      ),
    },
    {
      title: '账号',
      dataIndex: 'accountName',
      width: 130,
      ellipsis: true,
    },
    {
      title: '任务状态',
      dataIndex: 'status',
      width: 110,
      render: (status: PublishTaskStatus) => (
        <Tag color={statusColor(status)}>{getPublishTaskStatusLabel(status)}</Tag>
      ),
    },
    {
      title: '发布类型',
      dataIndex: 'publishType',
      width: 100,
      responsive: ['md'],
      render: (type: PublishType) => <Tag>{getPublishTypeLabel(type)}</Tag>,
    },
    {
      title: '发布方式',
      dataIndex: 'publishMode',
      width: 120,
      responsive: ['lg'],
      render: (mode: string) => <Tag>{getPublishModeLabel(mode)}</Tag>,
    },
    {
      title: '发布时间',
      key: 'publishTime',
      width: 150,
      responsive: ['xl'],
      render: (_, record) => getPublishDisplayTime(record),
    },
    {
      title: '发布结果',
      key: 'publishResult',
      width: 180,
      responsive: ['lg'],
      render: (_, record) => {
        if (record.status === 'SUCCESS' && isOfficialArticleUrl(record.publishUrl)) {
          if (record.platform === 'CSDN') {
            return (
              <Space direction="vertical" size={2} style={{ whiteSpace: 'normal' }}>
                <Typography.Text type="success">CSDN 发布成功</Typography.Text>
                <Typography.Link href={record.publishUrl} target="_blank" rel="noreferrer">
                  查看文章
                </Typography.Link>
              </Space>
            );
          }
          return <Typography.Link href={record.publishUrl} target="_blank" rel="noreferrer">查看文章</Typography.Link>;
        }
        if (record.status === 'SUCCESS' && record.platform === 'CSDN') {
          return (
            <Space direction="vertical" size={2} style={{ whiteSpace: 'normal' }}>
              <Typography.Text type="success">CSDN 发布成功</Typography.Text>
              {isCsdnManageUrl(record.publishUrl) || !record.publishUrl ? (
                <Typography.Text type="secondary">未自动获取正式文章链接，请在 CSDN 内容管理页查看。</Typography.Text>
              ) : null}
              <Typography.Link href={record.publishUrl || CSDN_MANAGE_URL} target="_blank" rel="noreferrer">
                查看文章
              </Typography.Link>
            </Space>
          );
        }
        if (record.status === 'SUCCESS' && isWechatDraftUrl(record.publishUrl)) {
          return <Typography.Text>草稿创建成功</Typography.Text>;
        }
        if (record.status === 'RUNNING' && record.platform === 'WECHAT_OFFICIAL' && record.externalPublishId) {
          return <Typography.Text>微信发布确认中</Typography.Text>;
        }
        if (record.status === 'RUNNING' && record.platform === 'CSDN') {
          return <Typography.Text type="warning">CSDN 浏览器自动化执行中，请不要操作 Chrome for Testing 窗口。</Typography.Text>;
        }
        if (record.status === 'NEED_MANUAL_CONFIRM') {
          if (record.platform === 'CSDN') {
            return (
              <Space direction="vertical" size={2} style={{ whiteSpace: 'normal' }}>
                <Typography.Text type="warning">已自动填充 CSDN 编辑器</Typography.Text>
                <Typography.Text type="secondary">请切换到 Chrome for Testing 窗口检查内容并手动发布。</Typography.Text>
                <Typography.Text type="secondary">如未看到按钮，请滚动到底部右下角查看“保存草稿 / 发布文章”。</Typography.Text>
              </Space>
            );
          }
          return (
            <Space direction="vertical" size={2}>
              <Typography.Text type="warning" ellipsis={{ tooltip: record.errorMessage }}>
                {record.errorMessage || '已自动填充，请在 Chrome for Testing 中检查并手动发布。'}
              </Typography.Text>
              {record.publishUrl ? (
                <Typography.Link href={record.publishUrl} target="_blank" rel="noreferrer">
                  打开编辑器
                </Typography.Link>
              ) : null}
            </Space>
          );
        }
        if (isFailureStatus(record.status)) {
          return (
            <Typography.Text type="danger" ellipsis={{ tooltip: record.errorMessage }}>
              {record.errorMessage || getPublishTaskStatusLabel(record.status) || '执行失败'}
            </Typography.Text>
          );
        }
        return '-';
      },
    },
    {
      title: '操作',
      key: 'action',
      width: 210,
      render: (_, record) => renderTaskActions(record),
    },
    {
      title: '文章状态',
      key: 'articleStatus',
      width: 130,
      render: (_, record) => {
        const status = articleStatus(record);
        return status ? <Tag color={status.color}>{status.label}</Tag> : '-';
      },
    },
  ];

  return (
    <PageContainer
      title="发布任务"
      description="创建平台发布稿的发布任务，提交后可手动执行自动发布或草稿创建。"
    >
      <SectionCard>
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <Alert
            type="info"
            showIcon
            message="JUEJIN + UNOFFICIAL_API 会访问真实掘金接口。"
            description="WECHAT_OFFICIAL + OFFICIAL_API 默认创建公众号草稿；账号配置 draftOnly=false 后会提交微信正式发布，发布后可点击刷新状态同步微信确认结果和掘金文章状态。"
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
            <Space>
              <Button loading={refreshing} onClick={handleRefreshStatus}>刷新状态</Button>
              <Button type="primary" onClick={() => openCreate()}>创建发布任务</Button>
            </Space>
          </Space>
          <Table
            rowKey="id"
            loading={loading}
            columns={columns}
            dataSource={tasks}
            locale={{
              emptyText: (
                <Empty description="暂无发布任务">
                  <Button type="primary" onClick={() => navigate('/articles')}>
                    去文章详情创建任务
                  </Button>
                </Empty>
              ),
            }}
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
