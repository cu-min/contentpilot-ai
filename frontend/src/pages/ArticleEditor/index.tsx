import { Button, Col, Form, Input, Modal, Row, Select, Space, Typography, message } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { createArticle, getArticleDetail, updateArticle } from '../../api/article';
import MarkdownPreview from '../../components/MarkdownPreview';
import PageContainer from '../../components/PageContainer';
import SectionCard from '../../components/SectionCard';
import type { ArticleCreatePayload } from '../../types/article';
import {
  articleLanguageOptions,
  articleTypeOptions,
  getArticleLanguageLabel,
  getArticleTypeLabel,
} from '../../types/article';
import { formatFailure } from '../../utils/feedback';
import PlatformContentSection from './PlatformContentSection';
import './style.css';

const { TextArea } = Input;

export default function ArticleEditor() {
  const navigate = useNavigate();
  const params = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const articleId = params.id ? Number(params.id) : null;
  const isEdit = Boolean(articleId);
  const [form] = Form.useForm<ArticleCreatePayload>();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [previewOpen, setPreviewOpen] = useState(false);
  const content = Form.useWatch('content', form);
  const openPlatformGenerate = searchParams.get('openPlatformGenerate') === '1';

  const pageDescription = useMemo(
    () => isEdit
      ? '打磨标题、摘要和正文，让一篇文章继续变成适合不同平台发布的内容。'
      : '从一篇新文章开始，把想法写成草稿，再继续适配到不同平台。',
    [isEdit],
  );

  useEffect(() => {
    if (!articleId) {
      form.setFieldsValue({
        type: 'PRODUCT_INTRO',
        language: 'ZH',
        content: '# 文章标题\n\n请输入正文内容。',
      });
      return;
    }

    setLoading(true);
    getArticleDetail(articleId)
      .then((result) => {
        form.setFieldsValue(result.data);
      })
      .catch((error) => {
        message.error(error instanceof Error ? error.message : '文章详情加载失败');
      })
      .finally(() => {
        setLoading(false);
      });
  }, [articleId, form]);

  const handleFinish = async (values: ArticleCreatePayload) => {
    setSaving(true);
    try {
      if (articleId) {
        await updateArticle(articleId, values);
        message.success('保存成功');
      } else {
        const result = await createArticle(values);
        message.success('保存成功');
        navigate(`/articles/${result.data.id}`, { replace: true });
      }
    } catch (error) {
      message.error(formatFailure('保存', error));
    } finally {
      setSaving(false);
    }
  };

  return (
    <PageContainer
      title={isEdit ? '文章详情' : '新建文章'}
      description={pageDescription}
    >
      <SectionCard className="article-editor-card" loading={loading}>
        <Typography.Text strong>文章内容</Typography.Text>
        <div style={{ height: 18 }} />
        <Form
          form={form}
          layout="vertical"
          onFinish={handleFinish}
          requiredMark="optional"
        >
          <Row gutter={18}>
            <Col xs={24} lg={14}>
              <Form.Item
                label="标题"
                name="title"
                rules={[{ required: true, message: '请输入文章标题' }]}
              >
                <Input placeholder="请输入文章标题" maxLength={200} />
              </Form.Item>
            </Col>
            <Col xs={24} lg={5}>
              <Form.Item
                label="文章类型"
                name="type"
                rules={[{ required: true, message: '请选择文章类型' }]}
              >
                <Select
                  options={articleTypeOptions.map((option) => ({
                    value: option.value,
                    label: getArticleTypeLabel(option.value),
                  }))}
                />
              </Form.Item>
            </Col>
            <Col xs={24} lg={5}>
              <Form.Item
                label="语言"
                name="language"
                rules={[{ required: true, message: '请选择语言' }]}
              >
                <Select
                  options={articleLanguageOptions.map((option) => ({
                    value: option.value,
                    label: getArticleLanguageLabel(option.value),
                  }))}
                />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item label="摘要" name="summary">
            <TextArea rows={3} placeholder="请输入文章摘要，便于列表快速识别内容主题" maxLength={500} showCount />
          </Form.Item>
          <Row gutter={18}>
            <Col xs={24} lg={12}>
              <Form.Item label="标签" name="tags">
                <Input placeholder="多个标签用逗号分隔，例如：AI工具,内容营销" maxLength={500} />
              </Form.Item>
            </Col>
            <Col xs={24} lg={12}>
              <Form.Item label="推荐关键词" name="keywords">
                <Input placeholder="多个关键词用逗号分隔，例如：AI写作,营销自动化" maxLength={500} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={18} align="stretch">
            <Col xs={24} xl={12}>
              <Form.Item
                label={
                  <Space size={6}>
                    <span>Markdown 正文</span>
                    <Typography.Text className="muted-text">建议填写正文内容</Typography.Text>
                  </Space>
                }
                name="content"
              >
                <TextArea
                  className="markdown-editor"
                  placeholder="支持基础 Markdown：# 标题、**加粗**、`代码`、- 列表、```代码块```"
                />
              </Form.Item>
            </Col>
            <Col xs={24} xl={12}>
              <div className="markdown-preview-panel markdown-preview-panel-limited">
                <div className="markdown-preview-panel-header">
                  <Typography.Text strong>Markdown 预览</Typography.Text>
                  <Button size="small" onClick={() => setPreviewOpen(true)}>展示全部</Button>
                </div>
                <MarkdownPreview value={content} />
              </div>
            </Col>
          </Row>
          <Space>
            <Button type="primary" htmlType="submit" loading={saving}>
              保存文章
            </Button>
            <Button onClick={() => navigate('/articles')}>返回列表</Button>
          </Space>
        </Form>
      </SectionCard>
      <Modal
        title="Markdown 预览"
        open={previewOpen}
        onCancel={() => setPreviewOpen(false)}
        footer={null}
        centered
        width={920}
      >
        <div className="markdown-preview-modal-body">
          <MarkdownPreview value={content} />
        </div>
      </Modal>
      {articleId ? (
        <PlatformContentSection
          articleId={articleId}
          autoOpenGenerate={openPlatformGenerate}
          onAutoOpenGenerateHandled={() => setSearchParams({})}
        />
      ) : null}
    </PageContainer>
  );
}
