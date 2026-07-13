import { Alert, Button, Descriptions, Form, Input, Select, Space, Typography, message } from 'antd';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { generateArticle } from '../../../api/ai';
import { getProductConfig } from '../../../api/productConfig';
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
  const [productConfig, setProductConfig] = useState<ProductConfig | null>(null);
  const [loadingConfig, setLoadingConfig] = useState(false);
  const [generating, setGenerating] = useState(false);

  const loadProductConfig = async () => {
    setLoadingConfig(true);
    try {
      const result = await getProductConfig();
      setProductConfig(result.data?.id ? result.data : null);
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
      description="输入一个主题，基于当前产品配置生成一篇可编辑的营销文章。"
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
            {productConfig ? (
              <Descriptions column={{ xs: 1, md: 2 }} size="small">
                <Descriptions.Item label="产品名称">{productConfig.productName || '-'}</Descriptions.Item>
                <Descriptions.Item label="品牌语气">{productConfig.brandTone || '-'}</Descriptions.Item>
                <Descriptions.Item label="目标用户">{productConfig.targetUsers || '-'}</Descriptions.Item>
                <Descriptions.Item label="官网链接">{productConfig.officialUrl || '-'}</Descriptions.Item>
              </Descriptions>
            ) : (
              <Alert
                type="warning"
                showIcon
                message="请先完成产品配置，系统才会开始生成文章。"
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
