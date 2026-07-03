import {
  Alert,
  Button,
  Checkbox,
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
import {
  getPlatformContentPlatformLabel,
  getPlatformContentStatusLabel,
  platformContentOptions,
  platformContentRuleSummaries,
} from '../../types/platformContent';

const { TextArea } = Input;

interface PlatformContentSectionProps {
  articleId: number;
}

export default function PlatformContentSection({ articleId }: PlatformContentSectionProps) {
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

  const handleGenerate = async (values: PlatformContentGeneratePayload) => {
    setGenerating(true);
    try {
      await generatePlatformContents(articleId, values);
      message.success('平台发布稿生成成功');
      setGenerateOpen(false);
      generateForm.resetFields();
      await loadItems();
    } catch (error) {
      message.error(error instanceof Error ? error.message : '平台发布稿生成失败');
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
      message.success('平台发布稿保存成功');
      setEditOpen(false);
      setCurrent(null);
      await loadItems();
    } catch (error) {
      message.error(error instanceof Error ? error.message : '平台发布稿保存失败');
    } finally {
      setSaving(false);
    }
  };

  const handleArchive = async (record: ArticlePlatformContent) => {
    try {
      await archivePlatformContent(record.id);
      message.success('平台发布稿已归档');
      await loadItems();
    } catch (error) {
      message.error(error instanceof Error ? error.message : '归档失败');
    }
  };

  const handleRestore = async (record: ArticlePlatformContent) => {
    try {
      await restorePlatformContent(record.id);
      message.success('平台发布稿已恢复');
      await loadItems();
    } catch (error) {
      message.error(error instanceof Error ? error.message : '恢复失败');
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
      ellipsis: true,
    },
    {
      title: '摘要',
      dataIndex: 'summary',
      ellipsis: true,
      responsive: ['lg'],
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
      responsive: ['md'],
      render: (value?: string) => value || '-',
    },
    {
      title: '操作',
      key: 'action',
      width: 260,
      render: (_, record) => (
        <Space size={8}>
          <Button size="small" onClick={() => openEdit(record)}>查看编辑</Button>
          <Button size="small" onClick={() => navigate(`/publish?platformContentId=${record.id}`)}>创建任务</Button>
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
        <Alert
          className="platform-content-rule-alert"
          type="info"
          showIcon
          message="平台适配会自动删减、重组和改写内容，不是简单复制原文。"
          description="系统会保留核心观点、产品价值、用户痛点、解决方案和温和转化引导，同时删除不适合目标平台的长铺垫、硬广表达和冗余段落。"
        />
        <Table
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={items}
          pagination={false}
          locale={{ emptyText: '暂无平台发布稿' }}
        />
      </SectionCard>

      <Modal
        title="生成平台发布稿"
        open={generateOpen}
        onCancel={() => setGenerateOpen(false)}
        onOk={() => generateForm.submit()}
        okText="开始生成"
        cancelText="取消"
        confirmLoading={generating}
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
            <div className="markdown-preview-panel">
              <Typography.Text strong>Markdown 预览</Typography.Text>
              <MarkdownPreview value={editingContent} />
            </div>
          </div>
        </Form>
      </Modal>
    </>
  );
}
