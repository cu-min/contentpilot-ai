import { Alert, Button, Descriptions, Form, Input, Select, Space, Typography, message } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { generateArticle } from '../../api/ai';
import { getProductConfig } from '../../api/productConfig';
import PageContainer from '../../components/PageContainer';
import SectionCard from '../../components/SectionCard';
import type { AiArticleGenerateRequest } from '../../types/ai';
import {
  articleLanguageOptions,
  articleTypeOptions,
  getArticleLanguageLabel,
  getArticleTypeLabel,
} from '../../types/article';
import type { ProductConfig } from '../../types/product';

const { TextArea } = Input;

export default function AiGenerate() {
  const navigate = useNavigate();
  const [form] = Form.useForm<AiArticleGenerateRequest>();
  const [productConfig, setProductConfig] = useState<ProductConfig | null>(null);
  const [loadingConfig, setLoadingConfig] = useState(false);
  const [generating, setGenerating] = useState(false);

  const productReady = useMemo(
    () => Boolean(productConfig?.productName?.trim()),
    [productConfig],
  );

  const loadProductConfig = async () => {
    setLoadingConfig(true);
    try {
      const result = await getProductConfig();
      setProductConfig(result.data);
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
    void loadProductConfig();
  }, [form]);

  const handleFinish = async (values: AiArticleGenerateRequest) => {
    setGenerating(true);
    try {
      const result = await generateArticle(values);
      message.success('文章生成成功');
      navigate(`/articles/${result.data.articleId}`);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '文章生成失败');
    } finally {
      setGenerating(false);
    }
  };

  return (
    <PageContainer
      title="AI文章生成"
      description="基于产品配置、文章主题、类型和语言生成原始营销文章，并自动保存到文章库草稿。"
    >
      <SectionCard loading={loadingConfig}>
        <Space direction="vertical" size={18} style={{ width: '100%' }}>
          <Space style={{ width: '100%', justifyContent: 'space-between' }}>
            <Typography.Text strong>产品配置摘要</Typography.Text>
            <Button onClick={() => navigate('/product')}>前往产品配置</Button>
          </Space>
          {productReady ? (
            <Descriptions column={{ xs: 1, md: 2 }} size="small">
              <Descriptions.Item label="产品名称">{productConfig?.productName || '-'}</Descriptions.Item>
              <Descriptions.Item label="品牌语气">{productConfig?.brandTone || '-'}</Descriptions.Item>
              <Descriptions.Item label="目标用户">{productConfig?.targetUsers || '-'}</Descriptions.Item>
              <Descriptions.Item label="官网链接">{productConfig?.officialUrl || '-'}</Descriptions.Item>
            </Descriptions>
          ) : (
            <Alert
              type="warning"
              showIcon
              message="请先完成产品配置后再生成文章。"
            />
          )}
        </Space>
      </SectionCard>

      <SectionCard>
        <Form
          form={form}
          layout="vertical"
          onFinish={handleFinish}
          requiredMark="optional"
        >
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
              disabled={!productReady}
            >
              生成文章
            </Button>
            <Button onClick={() => form.resetFields()}>重置</Button>
          </Space>
        </Form>
      </SectionCard>
    </PageContainer>
  );
}
