import { Alert, Button, Descriptions, Form, Input, Select, Space, Typography, message } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { generateArticle, getAiGenerationTask } from '../../../api/ai';
import { listProductConfigs } from '../../../api/productConfig';
import PageContainer from '../../../components/PageContainer';
import SectionCard from '../../../components/SectionCard';
import type { AiArticleGenerateRequest, AiGenerationTask } from '../../../types/ai';
import {
  articleLanguageOptions,
  articleTypeOptions,
  getArticleLanguageLabel,
  getArticleTypeLabel,
} from '../../../types/article';
import type { ProductConfig } from '../../../types/product';
import { formatFailure } from '../../../utils/feedback';

const { TextArea } = Input;

export default function ArticleAiGenerate() {
  const navigate = useNavigate();
  const [form] = Form.useForm<AiArticleGenerateRequest>();
  const selectedProductId = Form.useWatch('productConfigId', form);
  const [productConfigs, setProductConfigs] = useState<ProductConfig[]>([]);
  const [loadingConfig, setLoadingConfig] = useState(false);
  const [generating, setGenerating] = useState(false);
  const [taskId, setTaskId] = useState<number | null>(null);
  const [generationTask, setGenerationTask] = useState<AiGenerationTask | null>(null);

  const selectedProduct = useMemo(
    () => productConfigs.find((product) => product.id === selectedProductId) || null,
    [productConfigs, selectedProductId],
  );

  const loadProductConfigs = async () => {
    setLoadingConfig(true);
    try {
      const result = await listProductConfigs();
      setProductConfigs(result.data || []);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '产品配置加载失败');
    } finally {
      setLoadingConfig(false);
    }
  };

  useEffect(() => {
    form.setFieldsValue({
      type: 'INDUSTRY_KNOWLEDGE',
      language: 'ZH',
    });
    void loadProductConfigs();
  }, [form]);

  useEffect(() => {
    if (!taskId) {
      return undefined;
    }

    let cancelled = false;
    let timer: number | undefined;
    const poll = async () => {
      try {
        const result = await getAiGenerationTask(taskId);
        if (cancelled) {
          return;
        }
        const task = result.data;
        setGenerationTask(task);
        if (task.status === 'SUCCESS' && task.articleId) {
          setGenerating(false);
          message.success('文章生成成功');
          navigate(`/articles/${task.articleId}`);
          return;
        }
        if (task.status === 'FAILED') {
          setGenerating(false);
          setTaskId(null);
          message.error(task.errorMessage || '文章生成失败，请稍后重试');
          return;
        }
        timer = window.setTimeout(() => void poll(), 1600);
      } catch (error) {
        if (!cancelled) {
          setGenerating(false);
          setTaskId(null);
          message.error(formatFailure('查询生成进度', error));
        }
      }
    };
    void poll();
    return () => {
      cancelled = true;
      if (timer) {
        window.clearTimeout(timer);
      }
    };
  }, [navigate, taskId]);

  const handleFinish = async (values: AiArticleGenerateRequest) => {
    setGenerating(true);
    setGenerationTask(null);
    try {
      const result = await generateArticle(values);
      setTaskId(result.data.taskId);
      setGenerationTask({
        taskId: result.data.taskId,
        status: result.data.status,
        progressMessage: '任务已提交，等待联网取材',
      });
    } catch (error) {
      message.error(formatFailure('生成', error));
      setGenerating(false);
    }
  };

  return (
    <PageContainer
      title="AI生成文章"
      description="输入主题后系统会先联网取材；选择产品可额外注入品牌和功能上下文。"
    >
      <Form
        form={form}
        layout="vertical"
        onFinish={handleFinish}
        requiredMark="optional"
      >
        <SectionCard loading={loadingConfig}>
          <Space direction="vertical" size={18} style={{ width: '100%' }}>
            <Space style={{ width: '100%', justifyContent: 'space-between' }}>
              <Typography.Text strong>关联产品</Typography.Text>
              <Button onClick={() => navigate('/product')}>前往产品配置</Button>
            </Space>
            <Form.Item
              label="选择产品"
              name="productConfigId"
              extra="可选。未选择时，系统只按主题、类型、备注和联网资料生成通用文章。"
            >
              <Select
                allowClear
                showSearch
                placeholder="不选择产品，按主题生成"
                optionFilterProp="label"
                options={productConfigs.map((product) => ({
                  value: product.id,
                  label: product.productName,
                }))}
              />
            </Form.Item>
            {selectedProduct ? (
              <Descriptions column={{ xs: 1, md: 2 }} size="small">
                <Descriptions.Item label="产品名称">{selectedProduct.productName || '-'}</Descriptions.Item>
                <Descriptions.Item label="品牌语气">{selectedProduct.brandTone || '-'}</Descriptions.Item>
                <Descriptions.Item label="目标用户">{selectedProduct.targetUsers || '-'}</Descriptions.Item>
                <Descriptions.Item label="官网链接">{selectedProduct.officialUrl || '-'}</Descriptions.Item>
              </Descriptions>
            ) : (
              <Alert
                type="warning"
                showIcon
                message="当前未关联产品：将按主题和联网资料生成通用文章。"
              />
            )}
          </Space>
        </SectionCard>

        <SectionCard>
          <Typography.Text strong>生成参数</Typography.Text>
          <div style={{ height: 18 }} />
          {generationTask ? (
            <Alert
              type="info"
              showIcon
              style={{ marginBottom: 18 }}
              message={generationTask.progressMessage || '文章正在后台生成'}
              description="系统会先联网收集资料，再由 DeepSeek 生成文章。离开此页面后任务仍会继续运行。"
            />
          ) : null}
          <Form.Item
            label="文章主题"
            name="topic"
            rules={[{ required: true, message: '请输入文章主题' }]}
          >
            <Input placeholder="例如：研发团队为什么需要项目管理系统" maxLength={200} showCount />
          </Form.Item>
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
          <Form.Item label="额外要求" name="extraRequirement">
            <TextArea
              rows={5}
              maxLength={1000}
              showCount
              placeholder="可选。例如：文章要适合微信公众号和知乎阅读，减少硬广感"
            />
          </Form.Item>
          <Space>
            <Button
              type="primary"
              htmlType="submit"
              loading={generating}
              disabled={generating}
            >
              生成文章
            </Button>
            <Button
              onClick={() => form.setFieldsValue({
                productConfigId: undefined,
                topic: undefined,
                type: 'INDUSTRY_KNOWLEDGE',
                language: 'ZH',
                extraRequirement: undefined,
              })}
            >
              重置
            </Button>
          </Space>
        </SectionCard>
      </Form>
    </PageContainer>
  );
}
