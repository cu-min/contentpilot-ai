import { Alert, Button, Descriptions, Form, Input, Select, Space, Typography, message } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { generateArticle } from '../../../api/ai';
import { listProductConfigs } from '../../../api/productConfig';
import PageContainer from '../../../components/PageContainer';
import SectionCard from '../../../components/SectionCard';
import type { AiArticleGenerateRequest } from '../../../types/ai';
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

  const handleFinish = async (values: AiArticleGenerateRequest) => {
    setGenerating(true);
    message.info('文章后台自动生成中', 3);
    try {
      const result = await generateArticle(values);
      message.success('文章生成成功', 3);
      navigate(`/articles/${result.data.articleId}`);
    } catch (error) {
      message.error(formatFailure('生成', error));
    } finally {
      setGenerating(false);
    }
  };

  return (
    <PageContainer
      title="AI生成文章"
      description="输入一个主题，就能生成一篇可编辑的营销文章；选择产品后，内容会更贴近你的品牌表达。"
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
              extra="可选。不选择产品时，AI 将按文章主题直接生成，不会编造具体产品功能。"
            >
              <Select
                allowClear
                showSearch
                placeholder="不选择产品，直接按主题生成"
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
                type="info"
                showIcon
                message="当前未关联产品，将按文章主题生成通用内容。"
              />
            )}
          </Space>
        </SectionCard>

        <SectionCard>
          <Typography.Text strong>生成参数</Typography.Text>
          <div style={{ height: 18 }} />
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
