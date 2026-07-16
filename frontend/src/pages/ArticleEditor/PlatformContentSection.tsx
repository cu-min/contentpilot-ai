import {
  Button,
  Checkbox,
  Empty,
  Form,
  Input,
  Modal,
  Popconfirm,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import type { TableColumnsType } from 'antd';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  archivePlatformContent,
  generatePlatformContents,
  getArticlePlatformContents,
  getPlatformContentDetail,
  restorePlatformContent,
  updatePlatformContent,
} from '../../api/platformContent';
import MarkdownPreview from '../../components/MarkdownPreview';
import SectionCard from '../../components/SectionCard';
import type {
  ArticlePlatformContent,
  PlatformContentGeneratePayload,
  PlatformContentPlatform,
  PlatformContentUpdatePayload,
} from '../../types/platformContent';
import { formatDateTime } from '../../utils/datetime';
import { formatFailure } from '../../utils/feedback';
import {
  getPlatformContentPlatformLabel,
  getPlatformContentStatusLabel,
  platformContentOptions,
  platformContentRuleSummaries,
} from '../../types/platformContent';

const { TextArea } = Input;

interface PlatformContentSectionProps {
  articleId: number;
  autoOpenGenerate?: boolean;
  onAutoOpenGenerateHandled?: () => void;
}

export default function PlatformContentSection({
  articleId,
  autoOpenGenerate = false,
  onAutoOpenGenerateHandled,
}: PlatformContentSectionProps) {
  const navigate = useNavigate();
  const [generateForm] = Form.useForm<PlatformContentGeneratePayload>();
  const [editForm] = Form.useForm<PlatformContentUpdatePayload>();
  const [items, setItems] = useState<ArticlePlatformContent[]>([]);
  const [loading, setLoading] = useState(false);
  const [generating, setGenerating] = useState(false);
  const [generateOpen, setGenerateOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [current, setCurrent] = useState<ArticlePlatformContent | null>(null);
  const [fullPreviewOpen, setFullPreviewOpen] = useState(false);
  const editingContent = Form.useWatch('content', editForm);

  const loadItems = async () => {
    setLoading(true);
    try {
      const result = await getArticlePlatformContents(articleId);
      setItems(result.data);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '平台发布稿加载失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadItems();
  }, [articleId]);

  useEffect(() => {
    if (!autoOpenGenerate) {
      return;
    }
    setGenerateOpen(true);
    onAutoOpenGenerateHandled?.();
  }, [autoOpenGenerate, onAutoOpenGenerateHandled]);

  const handleGenerate = async (values: PlatformContentGeneratePayload) => {
    if (generating) {
      return;
    }
    setGenerating(true);
    message.info('平台稿后台自动生成中', 3);
    setGenerateOpen(false);
    generateForm.resetFields();
    try {
      await generatePlatformContents(articleId, values);
      message.success('平台稿生成成功', 3);
      await loadItems();
    } catch (error) {
      message.error(formatFailure('平台稿生成', error));
    } finally {
      setGenerating(false);
    }
  };

  const openEdit = async (record: ArticlePlatformContent) => {
    setEditOpen(true);
    setDetailLoading(true);
    try {
      const result = await getPlatformContentDetail(record.id);
      setCurrent(result.data);
      editForm.setFieldsValue(result.data);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '平台发布稿详情加载失败');
      setEditOpen(false);
    } finally {
      setDetailLoading(false);
    }
  };

  const handleSave = async () => {
    if (!current) {
      return;
    }
    const values = await editForm.validateFields();
    setSaving(true);
    try {
      await updatePlatformContent(current.id, values);
      message.success('保存成功');
      setEditOpen(false);
      setCurrent(null);
      await loadItems();
    } catch (error) {
      message.error(formatFailure('保存', error));
    } finally {
      setSaving(false);
    }
  };

  const handleArchive = async (record: ArticlePlatformContent) => {
    try {
      await archivePlatformContent(record.id);
      message.success('归档成功');
      await loadItems();
    } catch (error) {
      message.error(formatFailure('归档', error));
    }
  };

  const handleRestore = async (record: ArticlePlatformContent) => {
    try {
      await restorePlatformContent(record.id);
      message.success('恢复成功');
      await loadItems();
    } catch (error) {
      message.error(formatFailure('恢复', error));
    }
  };

  const columns: TableColumnsType<ArticlePlatformContent> = [
    {
      title: '平台',
      dataIndex: 'platform',
      width: 130,
      render: (platform: PlatformContentPlatform) => (
        <Tag color="processing">{getPlatformContentPlatformLabel(platform)}</Tag>
      ),
    },
    {
      title: '标题',
      dataIndex: 'title',
      width: 220,
      ellipsis: true,
    },
    {
      title: '摘要',
      dataIndex: 'summary',
      width: 260,
      ellipsis: true,
      responsive: ['xl'],
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 110,
      render: (status: string) => (
        <Tag color={status === 'ARCHIVED' ? 'default' : 'success'}>
          {getPlatformContentStatusLabel(status)}
        </Tag>
      ),
    },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      width: 180,
      responsive: ['lg'],
      render: (value?: string) => formatDateTime(value),
    },
    {
      title: '操作',
      key: 'action',
      width: 260,
      render: (_, record) => (
        <Space size={8}>
          <Button size="small" onClick={() => openEdit(record)}>查看编辑</Button>
          <Button size="small" onClick={() => navigate(`/publish?platformContentId=${record.id}`)}>准备发布</Button>
          {record.status === 'ARCHIVED' ? (
            <Button size="small" onClick={() => handleRestore(record)}>恢复</Button>
          ) : (
            <Popconfirm
              title="确认归档该平台发布稿？"
              okText="归档"
              cancelText="取消"
              onConfirm={() => handleArchive(record)}
            >
              <Button size="small">归档</Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  return (
    <>
      <SectionCard
        title="平台发布稿"
        extra={(
          <Button type="primary" onClick={() => setGenerateOpen(true)}>
            生成平台稿
          </Button>
        )}
      >
        <Table
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={items}
          pagination={false}
          locale={{
            emptyText: (
              <Empty description="暂无平台稿内容">
                <Button type="primary" onClick={() => setGenerateOpen(true)}>
                  生成平台稿
                </Button>
              </Empty>
            ),
          }}
        />
      </SectionCard>

      <Modal
        title="生成平台发布稿"
        open={generateOpen}
        onCancel={() => setGenerateOpen(false)}
        onOk={() => generateForm.submit()}
        okText="开始生成"
        cancelText="取消"
        width={760}
      >
        <Form
          form={generateForm}
          layout="vertical"
          onFinish={handleGenerate}
          initialValues={{ platforms: ['WECHAT_OFFICIAL', 'ZHIHU'] }}
        >
          <Form.Item
            label="目标平台"
            name="platforms"
            rules={[{ required: true, message: '请选择目标平台' }]}
          >
            <Checkbox.Group options={[...platformContentOptions]} />
          </Form.Item>
          <div className="platform-content-rule-grid">
            {platformContentRuleSummaries.map((rule) => (
              <div className="platform-content-rule-item" key={rule.platform}>
                <Space size={8}>
                  <Tag color="processing">{rule.name}</Tag>
                  <Typography.Text strong>{rule.goal}</Typography.Text>
                </Space>
                <Typography.Text className="muted-text">{rule.summary}</Typography.Text>
              </div>
            ))}
          </div>
          <Form.Item label="额外要求" name="extraRequirement">
            <TextArea
              rows={4}
              maxLength={1000}
              showCount
              placeholder="可选。例如：减少品牌露出，突出技术实践步骤"
            />
          </Form.Item>
          <Typography.Text className="muted-text">
            重复生成同一篇文章的同一平台稿时，会更新已有稿件。平台稿会按平台规则删减和重组，不会保留原文所有段落。
          </Typography.Text>
        </Form>
      </Modal>

      <Modal
        title={current ? `编辑 ${getPlatformContentPlatformLabel(current.platform)} 发布稿` : '编辑平台发布稿'}
        open={editOpen}
        onCancel={() => {
          setEditOpen(false);
          setCurrent(null);
        }}
        onOk={handleSave}
        okText="保存"
        cancelText="取消"
        confirmLoading={saving}
        width={1040}
      >
        <Form form={editForm} layout="vertical" requiredMark="optional" disabled={detailLoading}>
          <Form.Item
            label="标题"
            name="title"
            rules={[{ required: true, message: '请输入标题' }]}
          >
            <Input maxLength={200} showCount />
          </Form.Item>
          <Form.Item label="摘要" name="summary">
            <TextArea rows={3} maxLength={500} showCount />
          </Form.Item>
          <Space className="platform-content-meta-row" align="start">
            <Form.Item label="标签" name="tags">
              <Input maxLength={500} placeholder="多个标签用逗号分隔" />
            </Form.Item>
            <Form.Item label="关键词" name="keywords">
              <Input maxLength={500} placeholder="多个关键词用逗号分隔" />
            </Form.Item>
          </Space>
          <div className="platform-content-editor-grid">
            <Form.Item label="Markdown 正文" name="content">
              <TextArea className="platform-content-markdown-editor" />
            </Form.Item>
            <div className="markdown-preview-panel markdown-preview-panel-limited platform-content-preview-panel">
              <div className="markdown-preview-panel-header">
                <Typography.Text strong>Markdown 预览</Typography.Text>
                <Button size="small" onClick={() => setFullPreviewOpen(true)}>展示全部</Button>
              </div>
              <MarkdownPreview value={editingContent} />
            </div>
          </div>
        </Form>
      </Modal>
      <Modal
        title="Markdown 预览"
        open={fullPreviewOpen}
        onCancel={() => setFullPreviewOpen(false)}
        footer={null}
        centered
        width={920}
      >
        <div className="markdown-preview-modal-body">
          <MarkdownPreview value={editingContent} />
        </div>
      </Modal>
    </>
  );
}
