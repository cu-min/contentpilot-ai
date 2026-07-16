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
  Tooltip,
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
  getPublishTaskDetail,
  getPublishTasks,
  preparePublishTask,
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
import { formatFailure } from '../../utils/feedback';
import {
  getPreparedActionLabel,
  isHttpUrl,
  isPreparationReady,
  preparedFallbackUrls,
} from '../../utils/publishPreparation';

const CSDN_MANAGE_URL = preparedFallbackUrls.CSDN as string;
const ZHIHU_MANAGE_URL = preparedFallbackUrls.ZHIHU as string;
const NOVNC_URL = import.meta.env.VITE_NOVNC_URL
  || 'http://127.0.0.1:6080/vnc.html?autoconnect=true';

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
  const [preparingTaskId, setPreparingTaskId] = useState<number | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [submitModalOpen, setSubmitModalOpen] = useState(false);
  const [submittingTask, setSubmittingTask] = useState<PublishTask | null>(null);
  const [editing, setEditing] = useState<PublishTask | null>(null);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });

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

  const handleArticleChange = (articleId?: number) => {
    form.setFieldsValue({
      articleId,
      platformContentId: undefined,
      accountId: undefined,
      title: undefined,
    });
    setPlatformContents([]);
    setAccounts([]);
    if (articleId) {
      void loadPlatformContents(articleId);
    }
  };

  const handlePlatformContentChange = (platformContentId?: number) => {
    const content = platformContents.find((item) => item.id === platformContentId);
    form.setFieldsValue({
      platformContentId,
      accountId: undefined,
      title: content?.title,
    });
    setAccounts([]);
    if (content) {
      void loadAccounts(content.platform);
    }
  };

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
    setPlatformContents([]);
    setAccounts([]);
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
    setPlatformContents([]);
    setAccounts([]);
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
      setPlatformContents([]);
      setAccounts([]);
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
      submitForm.setFields([{ name: 'scheduleTime', errors: ['请选择未来的计划准备时间'] }]);
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
      message.success(values.publishType === 'SCHEDULED' ? '计划准备任务已提交' : '任务已提交，等待准备');
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

  const handlePrepare = async (record: PublishTask) => {
    setPreparingTaskId(record.id);
    try {
      const result = await preparePublishTask(record.id);
      if (result.data.status === 'WAITING_MANUAL_CONFIRM') {
        if (record.platform === 'WECHAT_OFFICIAL' || record.platform === 'JUEJIN') {
          message.success('平台草稿已准备，请检查后在平台页面人工发布');
        } else {
          message.success('浏览器内容已填充，请检查后在平台页面人工点击发布');
        }
      } else {
        message.error(`准备失败：${result.data.errorMessage || getPublishTaskStatusLabel(result.data.status) || '请稍后重试'}`);
      }
      await loadTasks();
    } catch (error) {
      message.error(formatFailure('准备发布', error));
    } finally {
      setPreparingTaskId(null);
    }
  };

  const isFailureStatus = (status: PublishTaskStatus) => status === 'FAILED';

  const statusColor = (status: PublishTaskStatus) => {
    if (status === 'DRAFT') return 'default';
    if (status === 'PENDING') return 'processing';
    if (status === 'RUNNING') return 'blue';
    if (status === 'WAITING_MANUAL_CONFIRM') return 'gold';
    if (status === 'SUCCESS') return 'success';
    if (isFailureStatus(status)) return 'error';
    if (status === 'CANCELLED') return 'warning';
    return 'default';
  };

  const articleStatus = (record: PublishTask) => {
    if (record.status === 'DRAFT' || record.status === 'PENDING') return null;
    if (record.status === 'RUNNING') return { label: '准备中', color: 'blue' };
    if (record.status === 'WAITING_MANUAL_CONFIRM') return { label: '等待人工确认', color: 'gold' };
    if (record.articleStatus === 'PUBLISHED') return { label: '已发布', color: 'success' };
    if (record.articleStatus === 'FAILED') return { label: '发布失败', color: 'error' };
    if (record.articleStatus === 'CANCELLED') return { label: '已取消', color: 'default' };
    if (record.status === 'SUCCESS') return { label: '已发布', color: 'success' };
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

  const isScheduleReady = (record: PublishTask) => (
    isPreparationReady(record.publishType, record.scheduleTime)
  );

  const handleOpenPrepared = (record: PublishTask) => {
    if (record.platform === 'CSDN' || record.platform === 'ZHIHU') {
      window.open(NOVNC_URL, '_blank', 'noreferrer');
      return;
    }
    if (isHttpUrl(record.draftUrl)) {
      window.open(record.draftUrl, '_blank', 'noreferrer');
      return;
    }
    if (record.platform === 'WECHAT_OFFICIAL') {
      const mediaId = record.externalDraftId || record.draftUrl?.replace('wechat-draft:', '');
      message.info(mediaId ? `微信公众号草稿 media_id：${mediaId}` : '草稿已创建，请登录微信公众号后台查看');
      return;
    }
    const fallbackUrl = preparedFallbackUrls[record.platform];
    if (fallbackUrl) {
      window.open(fallbackUrl, '_blank', 'noreferrer');
    }
  };

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
      const scheduleReady = isScheduleReady(record);
      const prepareButton = (
        <Button
          size="small"
          type="primary"
          disabled={!scheduleReady}
          loading={preparingTaskId === record.id}
        >
          准备发布
        </Button>
      );
      return (
        <Space size={8} wrap>
          {scheduleReady ? (
            <Popconfirm
              title="系统只会准备草稿或填充编辑器，不会点击最终发布按钮。确认开始准备？"
              okText="开始准备"
              cancelText="取消"
              onConfirm={() => handlePrepare(record)}
            >
              {prepareButton}
            </Popconfirm>
          ) : (
            <Tooltip title={`计划准备时间为 ${formatDateTime(record.scheduleTime)}，到达后才能开始准备`}>
              <span>{prepareButton}</span>
            </Tooltip>
          )}
          <Popconfirm title="确认取消该发布任务？" okText="取消任务" cancelText="返回" onConfirm={() => handleCancel(record)}>
            <Button size="small">取消</Button>
          </Popconfirm>
        </Space>
      );
    }

    if (record.status === 'RUNNING') {
      return <Button size="small" disabled>准备中</Button>;
    }

    if (record.status === 'WAITING_MANUAL_CONFIRM') {
      return <Button size="small" type="primary" onClick={() => handleOpenPrepared(record)}>{getPreparedActionLabel(record.platform, record.draftUrl)}</Button>;
    }

    if (record.status === 'SUCCESS') {
      return <Button size="small" type={record.platform === 'CSDN' || record.platform === 'ZHIHU' ? 'primary' : 'default'} onClick={() => handleViewResult(record)}>查看文章</Button>;
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
      title: '准备类型',
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
      title: '计划时间',
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
        if (record.status === 'WAITING_MANUAL_CONFIRM') {
          if (record.platform === 'WECHAT_OFFICIAL' || record.platform === 'JUEJIN') {
            const draftId = record.externalDraftId || (
              record.platform === 'WECHAT_OFFICIAL'
                ? record.draftUrl?.replace('wechat-draft:', '')
                : undefined
            );
            return (
              <Space direction="vertical" size={2} style={{ whiteSpace: 'normal' }}>
                <Typography.Text type="warning">平台草稿已准备</Typography.Text>
                <Typography.Text type="secondary">请检查草稿，并在平台页面人工确认发布。</Typography.Text>
                {draftId ? (
                  <Typography.Text type="secondary" copyable={{ text: draftId }}>
                    草稿 ID：{draftId}
                  </Typography.Text>
                ) : null}
              </Space>
            );
          }
          return (
            <Space direction="vertical" size={2} style={{ whiteSpace: 'normal' }}>
              <Typography.Text type="warning">编辑器已填充</Typography.Text>
              <Typography.Text type="secondary">系统已停在最终发布按钮前，请在浏览器检查并人工点击发布。</Typography.Text>
            </Space>
          );
        }
        if (record.status === 'SUCCESS' && isOfficialArticleUrl(record.publishUrl)) {
          if (record.platform === 'ZHIHU') {
            return (
              <Space direction="vertical" size={2} style={{ whiteSpace: 'normal' }}>
                <Typography.Text type="success">历史任务已完成</Typography.Text>
              </Space>
            );
          }
          if (record.platform === 'CSDN') {
            return (
              <Space direction="vertical" size={2} style={{ whiteSpace: 'normal' }}>
                <Typography.Text type="success">历史任务已完成</Typography.Text>
              </Space>
            );
          }
          return <Typography.Link href={record.publishUrl} target="_blank" rel="noreferrer">查看文章</Typography.Link>;
        }
        if (record.status === 'SUCCESS' && record.platform === 'ZHIHU') {
          return (
            <Space direction="vertical" size={2} style={{ whiteSpace: 'normal' }}>
              <Typography.Text type="success">历史任务已完成</Typography.Text>
              {!isZhihuArticleUrl(record.publishUrl) ? (
                <Typography.Text type="secondary">未自动获取正式文章链接，请在知乎创作中心查看。</Typography.Text>
              ) : null}
            </Space>
          );
        }
        if (record.status === 'SUCCESS' && record.platform === 'CSDN') {
          return (
            <Space direction="vertical" size={2} style={{ whiteSpace: 'normal' }}>
              <Typography.Text type="success">历史任务已完成</Typography.Text>
              {!isCsdnArticleUrl(record.publishUrl) ? (
                <Typography.Text type="secondary">未自动获取正式文章链接，请在 CSDN 内容管理页查看。</Typography.Text>
              ) : null}
            </Space>
          );
        }
        if (record.status === 'SUCCESS' && isWechatDraftUrl(record.publishUrl)) {
          return <Typography.Text>草稿创建成功</Typography.Text>;
        }
        if (record.status === 'RUNNING' && record.platform === 'CSDN') {
          return (
            <Space direction="vertical" size={2} style={{ whiteSpace: 'normal' }}>
              <Typography.Text type="warning">CSDN 编辑器准备中</Typography.Text>
              <Typography.Text type="secondary">请不要操作 Chrome for Testing 窗口。</Typography.Text>
            </Space>
          );
        }
        if (record.status === 'RUNNING' && record.platform === 'ZHIHU') {
          return (
            <Space direction="vertical" size={2} style={{ whiteSpace: 'normal' }}>
              <Typography.Text type="warning">知乎编辑器准备中</Typography.Text>
              <Typography.Text type="secondary">请不要操作 Chrome for Testing 窗口。</Typography.Text>
            </Space>
          );
        }
        if (isFailureStatus(record.status)) {
          if (record.platform === 'ZHIHU') {
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
              {record.errorMessage || getPublishTaskStatusLabel(record.status) || '准备失败'}
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
      description="系统负责创建平台草稿或填充编辑器，并停在最终发布按钮前；请由运营人员检查后在平台页面人工确认发布。"
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
                placeholder="准备类型"
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
              <Button type="primary" onClick={() => openCreate()}>创建准备任务</Button>
            </Space>
          </Space>
          <Table
            rowKey="id"
            loading={loading}
            columns={columns}
            dataSource={tasks}
            locale={{
              emptyText: (
                <Empty description="暂无准备任务">
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
              allowClear
              placeholder="请选择文章"
              optionFilterProp="label"
              options={articles.map((article) => ({ label: article.title, value: article.id }))}
              onChange={handleArticleChange}
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
              onChange={handlePlatformContentChange}
            />
          </Form.Item>
          <Form.Item
            label="平台账号"
            name="accountId"
            rules={[{ required: true, message: '请选择平台账号' }]}
            extra={selectedPlatformContent ? '只显示与当前平台稿同平台的账号。' : '请先选择平台发布稿。'}
          >
            <Select
              disabled={!selectedPlatformContent}
              placeholder="请选择平台账号"
              options={accounts
                .filter((account) => account.enabled === 1 && account.platform === selectedPlatformContent?.platform)
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
        title="提交准备任务"
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
            label="准备类型"
            name="publishType"
            rules={[{ required: true, message: '请选择准备类型' }]}
          >
            <Radio.Group options={[...publishTypeOptions]} />
          </Form.Item>
          {selectedSubmitPublishType === 'SCHEDULED' ? (
            <Form.Item
              label="计划准备时间"
              name="scheduleTime"
              rules={[{ required: true, message: '请选择计划准备时间' }]}
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
