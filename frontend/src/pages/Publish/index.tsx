import {
  Button,
  DatePicker,
  Empty,
  Form,
  Input,
  Modal,
  Popconfirm,
  Radio,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import type { TableColumnsType } from 'antd';
import dayjs, { type Dayjs } from 'dayjs';
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
import type {
  PublishTask,
  PublishTaskPayload,
  PublishTaskStatus,
  PublishTaskSubmitPayload,
  PublishType,
} from '../../types/publishTask';
import {
  getPublishTaskStatusLabel,
  getPublishTypeLabel,
  publishTaskStatusOptions,
  publishTypeOptions,
} from '../../types/publishTask';
import { formatDateTime } from '../../utils/datetime';
import { formatFailure, getErrorText } from '../../utils/feedback';

const CSDN_MANAGE_URL = 'https://mp.csdn.net/mp_blog/manage/article';
const ZHIHU_MANAGE_URL = 'https://www.zhihu.com/creator';

interface PublishTaskFormValues {
  articleId?: number;
  platformContentId: number;
  accountId: number;
  title?: string;
}

interface PublishTaskSubmitFormValues {
  publishType: PublishType;
  scheduleTime?: Dayjs;
}

export default function Publish() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [form] = Form.useForm<PublishTaskFormValues>();
  const [submitForm] = Form.useForm<PublishTaskSubmitFormValues>();
  const [filterForm] = Form.useForm();
  const [tasks, setTasks] = useState<PublishTask[]>([]);
  const [articles, setArticles] = useState<ArticleListItem[]>([]);
  const [platformContents, setPlatformContents] = useState<ArticlePlatformContent[]>([]);
  const [accounts, setAccounts] = useState<PlatformAccount[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [executingTaskId, setExecutingTaskId] = useState<number | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [submitModalOpen, setSubmitModalOpen] = useState(false);
  const [submittingTask, setSubmittingTask] = useState<PublishTask | null>(null);
  const [editing, setEditing] = useState<PublishTask | null>(null);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });

  const selectedArticleId = Form.useWatch('articleId', form);
  const selectedPlatformContentId = Form.useWatch('platformContentId', form);
  const selectedSubmitPublishType = Form.useWatch('publishType', submitForm);

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
    if (!tasks.some((task) => task.status === 'PENDING' && task.publishType === 'SCHEDULED')) {
      return undefined;
    }
    const timer = window.setInterval(() => {
      void loadTasks(pagination.current, pagination.pageSize);
    }, 10000);
    return () => window.clearInterval(timer);
  }, [tasks, pagination.current, pagination.pageSize]);

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
    const articleId = Number(searchParams.get('articleId'));
    if (platformContentId) {
      void openCreate(platformContentId);
      setSearchParams({});
      return;
    }
    if (articleId) {
      void openCreateForArticle(articleId);
      setSearchParams({});
    }
  }, [searchParams, setSearchParams]);

  const openCreate = async (platformContentId?: number) => {
    setEditing(null);
    form.resetFields();
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
      });
      await loadPlatformContents(content.articleId);
      await loadAccounts(content.platform);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '平台稿详情加载失败');
    }
  };

  const openCreateForArticle = async (articleId: number) => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({
      articleId,
      platformContentId: undefined,
      accountId: undefined,
      title: undefined,
    });
    setModalOpen(true);
    await loadPlatformContents(articleId);
    await loadAccounts();
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
      });
      setModalOpen(true);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '发布任务详情加载失败');
    }
  };

  const normalizePayload = (values: PublishTaskFormValues): PublishTaskPayload => {
    return {
      platformContentId: values.platformContentId,
      accountId: values.accountId,
      title: values.title,
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

  const openSubmit = (record: PublishTask) => {
    setSubmittingTask(record);
    submitForm.resetFields();
    submitForm.setFieldsValue({ publishType: 'IMMEDIATE' });
    setSubmitModalOpen(true);
  };

  const handleSubmit = async () => {
    if (!submittingTask) {
      return;
    }
    const values = await submitForm.validateFields();
    const now = dayjs();
    if (values.publishType === 'SCHEDULED' && (!values.scheduleTime || !values.scheduleTime.isAfter(now))) {
      submitForm.setFields([{ name: 'scheduleTime', errors: ['请选择未来的发布时间'] }]);
      return;
    }
    const payload: PublishTaskSubmitPayload = {
      publishType: values.publishType,
      scheduleTime: values.publishType === 'SCHEDULED'
        ? values.scheduleTime?.format('YYYY-MM-DDTHH:mm:ss')
        : undefined,
    };
    setSubmitting(true);
    try {
      await submitPublishTask(submittingTask.id, payload);
      message.success(values.publishType === 'SCHEDULED' ? '定时发布任务已提交' : '任务提交成功');
      setSubmitModalOpen(false);
      setSubmittingTask(null);
      submitForm.resetFields();
      await loadTasks();
    } catch (error) {
      message.error(formatFailure('任务提交', error));
    } finally {
      setSubmitting(false);
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
        if (record.platform === 'WECHAT_OFFICIAL') {
          message.success('公众号草稿创建成功');
        } else if (record.platform === 'ZHIHU') {
          message.success('知乎发布成功');
        } else {
          message.success('自动发布成功');
        }
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
    if (record.status === 'RUNNING' && record.platform === 'ZHIHU') {
      return { label: '知乎自动化中', color: 'blue' };
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
    && url.includes('/article/details/'),
  );

  const isZhihuArticleUrl = (url?: string) => Boolean(
    url
    && !url.includes('/edit')
    && !url.includes('/write')
    && (
      url.includes('zhuanlan.zhihu.com/p/')
      || (url.includes('zhihu.com') && url.includes('/p/'))
      || (url.includes('zhihu.com/question/') && url.includes('/answer/'))
    ),
  );

  const isOfficialArticleUrl = (url?: string) => isJuejinArticleUrl(url) || isCsdnArticleUrl(url) || isZhihuArticleUrl(url);

  const isWechatDraftUrl = (url?: string) => Boolean(url?.startsWith('wechat-draft:'));

  const isWechatPublishUrl = (url?: string) => Boolean(url?.startsWith('wechat-publish:'));

  const handleViewResult = (record: PublishTask) => {
    if (record.platform === 'CSDN') {
      const url = isCsdnArticleUrl(record.publishUrl)
        ? record.publishUrl
        : CSDN_MANAGE_URL;
      window.open(url || CSDN_MANAGE_URL, '_blank', 'noreferrer');
      return;
    }
    if (record.platform === 'ZHIHU') {
      const url = isZhihuArticleUrl(record.publishUrl)
        ? record.publishUrl
        : ZHIHU_MANAGE_URL;
      window.open(url || ZHIHU_MANAGE_URL, '_blank', 'noreferrer');
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
      content: (
        <Typography.Text style={{ whiteSpace: 'pre-wrap' }}>
          {record.errorMessage || getPublishTaskStatusLabel(record.status) || '暂无失败原因'}
        </Typography.Text>
      ),
    });
  };

  const handleViewManualConfirm = (record: PublishTask) => {
    Modal.info({
      title: '人工确认',
      content: record.errorMessage || '已自动填充，请在 Chrome for Testing 中检查并手动发布。',
    });
  };

  const compactFailureText = (record: PublishTask) => {
    const text = (record.errorMessage || getPublishTaskStatusLabel(record.status) || '请查看失败原因')
      .replace(/\s+/g, ' ')
      .trim();
    return text.length > 34 ? `${text.slice(0, 34)}...` : text;
  };

  const renderCompactFailureText = (record: PublishTask) => (
    <Typography.Text
      type="secondary"
      ellipsis={{ tooltip: record.errorMessage }}
      style={{ display: 'block', maxWidth: 180, whiteSpace: 'nowrap' }}
    >
      {compactFailureText(record)}
    </Typography.Text>
  );

  const getPublishDisplayTime = (record: PublishTask) => {
    if (record.publishType === 'SCHEDULED' && record.scheduleTime) {
      return formatDateTime(record.scheduleTime);
    }
    return formatDateTime(record.updatedAt || record.createdAt);
  };

  const disableScheduleDate = (current: Dayjs) => current.isBefore(dayjs(), 'day');

  const renderTaskActions = (record: PublishTask) => {
    if (record.status === 'DRAFT') {
      return (
        <Space size={8} wrap>
          <Button size="small" onClick={() => openEdit(record)}>编辑</Button>
          <Button size="small" type="primary" onClick={() => openSubmit(record)}>提交</Button>
          <Popconfirm title="确认取消该发布任务？" okText="取消任务" cancelText="返回" onConfirm={() => handleCancel(record)}>
            <Button size="small">取消</Button>
          </Popconfirm>
        </Space>
      );
    }

    if (record.status === 'PENDING') {
      if (record.publishType === 'SCHEDULED') {
        return (
          <Space size={8} wrap>
            <Typography.Text type="secondary">等待定时发布</Typography.Text>
            <Popconfirm title="确认取消该定时发布任务？" okText="取消任务" cancelText="返回" onConfirm={() => handleCancel(record)}>
              <Button size="small">取消</Button>
            </Popconfirm>
          </Space>
        );
      }
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
      if (record.platform === 'CSDN') return <Button size="small" disabled>CSDN 自动化中</Button>;
      if (record.platform === 'ZHIHU') return <Button size="small" disabled>知乎自动化中</Button>;
      return <Button size="small" disabled>发布中</Button>;
    }

    if (record.status === 'SUCCESS') {
      return <Button size="small" type={record.platform === 'CSDN' || record.platform === 'ZHIHU' ? 'primary' : 'default'} onClick={() => handleViewResult(record)}>查看文章</Button>;
    }

    if (record.status === 'NEED_MANUAL_CONFIRM') {
      if (record.platform === 'ZHIHU') {
        return <Button size="small" type="primary" onClick={() => handleViewResult(record)}>查看文章</Button>;
      }
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
          if (record.platform === 'ZHIHU') {
            return (
              <Space direction="vertical" size={2} style={{ whiteSpace: 'normal' }}>
                <Typography.Text type="success">知乎发布成功</Typography.Text>
              </Space>
            );
          }
          if (record.platform === 'CSDN') {
            return (
              <Space direction="vertical" size={2} style={{ whiteSpace: 'normal' }}>
                <Typography.Text type="success">CSDN 发布成功</Typography.Text>
              </Space>
            );
          }
          return <Typography.Link href={record.publishUrl} target="_blank" rel="noreferrer">查看文章</Typography.Link>;
        }
        if (record.status === 'SUCCESS' && record.platform === 'ZHIHU') {
          return (
            <Space direction="vertical" size={2} style={{ whiteSpace: 'normal' }}>
              <Typography.Text type="success">知乎发布成功</Typography.Text>
              {!isZhihuArticleUrl(record.publishUrl) ? (
                <Typography.Text type="secondary">未自动获取正式文章链接，请在知乎创作中心查看。</Typography.Text>
              ) : null}
            </Space>
          );
        }
        if (record.status === 'SUCCESS' && record.platform === 'CSDN') {
          return (
            <Space direction="vertical" size={2} style={{ whiteSpace: 'normal' }}>
              <Typography.Text type="success">CSDN 发布成功</Typography.Text>
              {!isCsdnArticleUrl(record.publishUrl) ? (
                <Typography.Text type="secondary">未自动获取正式文章链接，请在 CSDN 内容管理页查看。</Typography.Text>
              ) : null}
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
          return (
            <Space direction="vertical" size={2} style={{ whiteSpace: 'normal' }}>
              <Typography.Text type="warning">CSDN 浏览器自动化执行中</Typography.Text>
              <Typography.Text type="secondary">请不要操作 Chrome for Testing 窗口。</Typography.Text>
            </Space>
          );
        }
        if (record.status === 'RUNNING' && record.platform === 'ZHIHU') {
          return (
            <Space direction="vertical" size={2} style={{ whiteSpace: 'normal' }}>
              <Typography.Text type="warning">知乎浏览器自动化执行中</Typography.Text>
              <Typography.Text type="secondary">请不要操作 Chrome for Testing 窗口。</Typography.Text>
            </Space>
          );
        }
        if (record.status === 'NEED_MANUAL_CONFIRM') {
          if (record.platform === 'ZHIHU') {
            return (
              <Space direction="vertical" size={2} style={{ whiteSpace: 'normal' }}>
                <Typography.Text type="warning">已自动填充知乎编辑器</Typography.Text>
                <Typography.Text type="secondary">请切换到 Chrome for Testing 窗口检查并手动发布。</Typography.Text>
              </Space>
            );
          }
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
          if (record.platform === 'ZHIHU') {
            if (record.status === 'NEED_LOGIN') {
              return (
                <Space direction="vertical" size={2} style={{ whiteSpace: 'normal' }}>
                  <Typography.Text type="danger">知乎登录状态失效</Typography.Text>
                  <Typography.Text type="secondary">请在打开的浏览器中完成登录后重新执行发布任务。</Typography.Text>
                </Space>
              );
            }
            if (record.status === 'NEED_CAPTCHA') {
              return (
                <Space direction="vertical" size={2} style={{ whiteSpace: 'normal' }}>
                  <Typography.Text type="danger">知乎需要验证码或安全验证</Typography.Text>
                  <Typography.Text type="secondary">请人工处理后重新执行。</Typography.Text>
                </Space>
              );
            }
            return (
              <Space direction="vertical" size={2} style={{ whiteSpace: 'normal' }}>
                <Typography.Text type="danger">知乎发布失败</Typography.Text>
                {renderCompactFailureText(record)}
              </Space>
            );
          }
          if (record.platform === 'CSDN') {
            return (
              <Space direction="vertical" size={2} style={{ whiteSpace: 'normal' }}>
                <Typography.Text type="danger">CSDN 发布失败</Typography.Text>
                {renderCompactFailureText(record)}
              </Space>
            );
          }
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
      description="把准备好的平台稿送出去：创建任务、自动发布、查看结果，每一步都清清楚楚。"
    >
      <SectionCard>
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
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
        </Form>
      </Modal>

      <Modal
        title="提交发布任务"
        open={submitModalOpen}
        onCancel={() => {
          setSubmitModalOpen(false);
          setSubmittingTask(null);
          submitForm.resetFields();
        }}
        onOk={handleSubmit}
        okText="确认提交"
        cancelText="取消"
        confirmLoading={submitting}
        destroyOnClose
      >
        <Form form={submitForm} layout="vertical" requiredMark="optional">
          <Form.Item
            label="发布类型"
            name="publishType"
            rules={[{ required: true, message: '请选择发布类型' }]}
          >
            <Radio.Group options={[...publishTypeOptions]} />
          </Form.Item>
          {selectedSubmitPublishType === 'SCHEDULED' ? (
            <Form.Item
              label="计划发布时间"
              name="scheduleTime"
              rules={[{ required: true, message: '请选择计划发布时间' }]}
            >
              <DatePicker
                showTime
                format="YYYY-MM-DD HH:mm:ss"
                disabledDate={disableScheduleDate}
                style={{ width: '100%' }}
              />
            </Form.Item>
          ) : null}
        </Form>
      </Modal>
    </PageContainer>
  );
}
