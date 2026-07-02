import { Button, Form, Input, Modal, Select, Space, Table, Typography, message } from 'antd';
import type { TableProps } from 'antd';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { archiveArticle, getArticles, restoreArticle } from '../../api/article';
import { ArticleTypeTag, LanguageTag, StatusTag } from '../../components/ArticleMetaTag';
import PageContainer from '../../components/PageContainer';
import SectionCard from '../../components/SectionCard';
import type { ArticleListItem, ArticleQuery } from '../../types/article';
import {
  articleLanguageOptions,
  articleStatusOptions,
  articleTypeOptions,
  getArticleLanguageLabel,
  getArticleStatusLabel,
  getArticleTypeLabel,
} from '../../types/article';

export default function Article() {
  const navigate = useNavigate();
  const [form] = Form.useForm<ArticleQuery>();
  const [articles, setArticles] = useState<ArticleListItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });

  const loadArticles = async (page = pagination.current, size = pagination.pageSize) => {
    setLoading(true);
    try {
      const values = form.getFieldsValue();
      const result = await getArticles({
        page,
        size,
        keyword: values.keyword,
        status: values.status,
        type: values.type,
        language: values.language,
      });
      setArticles(result.data.records);
      setPagination({
        current: result.data.current,
        pageSize: result.data.size,
        total: result.data.total,
      });
    } catch (error) {
      message.error(error instanceof Error ? error.message : '文章列表加载失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadArticles(1, 10);
  }, []);

  const handleArchive = (article: ArticleListItem) => {
    Modal.confirm({
      title: '归档文章',
      content: `确认归档「${article.title}」吗？归档后仍可恢复为草稿。`,
      okText: '归档',
      cancelText: '取消',
      onOk: async () => {
        await archiveArticle(article.id);
        message.success('文章已归档');
        await loadArticles();
      },
    });
  };

  const handleRestore = async (article: ArticleListItem) => {
    try {
      await restoreArticle(article.id);
      message.success('文章已恢复为草稿');
      await loadArticles();
    } catch (error) {
      message.error(error instanceof Error ? error.message : '文章恢复失败');
    }
  };

  const columns: TableProps<ArticleListItem>['columns'] = [
    {
      title: '标题',
      dataIndex: 'title',
      width: 220,
      render: (value, record) => (
        <Button type="link" className="table-link-button" onClick={() => navigate(`/articles/${record.id}`)}>
          {value}
        </Button>
      ),
    },
    {
      title: '摘要',
      dataIndex: 'summary',
      ellipsis: true,
      render: (value) => value || '-',
    },
    {
      title: '类型',
      dataIndex: 'type',
      width: 120,
      render: (type) => <ArticleTypeTag type={type} />,
    },
    {
      title: '语言',
      dataIndex: 'language',
      width: 90,
      render: (language) => <LanguageTag language={language} />,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 110,
      render: (status) => <StatusTag status={status} />,
    },
    {
      title: '标签',
      dataIndex: 'tags',
      ellipsis: true,
      render: (value) => value || '-',
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      width: 170,
      render: (value) => value ? value.replace('T', ' ') : '-',
    },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      width: 170,
      render: (value) => value ? value.replace('T', ' ') : '-',
    },
    {
      title: '操作',
      key: 'actions',
      fixed: 'right',
      width: 170,
      render: (_, record) => (
        <Space size={4}>
          <Button type="link" onClick={() => navigate(`/articles/${record.id}`)}>
            查看/编辑
          </Button>
          {record.status === 'ARCHIVED' ? (
            <Button type="link" onClick={() => void handleRestore(record)}>
              恢复
            </Button>
          ) : (
            <Button type="link" danger onClick={() => handleArchive(record)}>
              归档
            </Button>
          )}
        </Space>
      ),
    },
  ];

  return (
    <PageContainer
      title="文章库"
      description="管理手动创建和编辑的营销文章。当前阶段用于打通文章列表、详情、Markdown 编辑与预览。"
    >
      <SectionCard className="article-list-card">
        <Space direction="vertical" size={18} style={{ width: '100%' }}>
          <Space style={{ width: '100%', justifyContent: 'space-between' }}>
            <Typography.Text strong>文章列表</Typography.Text>
            <Button type="primary" onClick={() => navigate('/articles/new')}>
              新建文章
            </Button>
          </Space>
          <Form form={form} layout="inline" onFinish={() => void loadArticles(1, pagination.pageSize)}>
            <Form.Item name="keyword">
              <Input.Search placeholder="搜索标题、摘要、标签或关键词" allowClear onSearch={() => void loadArticles(1, pagination.pageSize)} />
            </Form.Item>
            <Form.Item name="status">
              <Select
                allowClear
                placeholder="状态"
                style={{ width: 130 }}
                options={articleStatusOptions.map((option) => ({
                  value: option.value,
                  label: getArticleStatusLabel(option.value),
                }))}
              />
            </Form.Item>
            <Form.Item name="type">
              <Select
                allowClear
                placeholder="类型"
                style={{ width: 150 }}
                options={articleTypeOptions.map((option) => ({
                  value: option.value,
                  label: getArticleTypeLabel(option.value),
                }))}
              />
            </Form.Item>
            <Form.Item name="language">
              <Select
                allowClear
                placeholder="语言"
                style={{ width: 110 }}
                options={articleLanguageOptions.map((option) => ({
                  value: option.value,
                  label: getArticleLanguageLabel(option.value),
                }))}
              />
            </Form.Item>
            <Form.Item>
              <Space>
                <Button type="primary" htmlType="submit">
                  查询
                </Button>
                <Button
                  onClick={() => {
                    form.resetFields();
                    void loadArticles(1, pagination.pageSize);
                  }}
                >
                  重置
                </Button>
              </Space>
            </Form.Item>
          </Form>
          <Table
            rowKey="id"
            loading={loading}
            columns={columns}
            dataSource={articles}
            scroll={{ x: 1260 }}
            pagination={{
              current: pagination.current,
              pageSize: pagination.pageSize,
              total: pagination.total,
              onChange: (page, pageSize) => void loadArticles(page, pageSize),
            }}
          />
        </Space>
      </SectionCard>
    </PageContainer>
  );
}
