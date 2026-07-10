import { Button, Empty, Form, Input, Modal, Select, Space, Table, Typography, message } from 'antd';
import type { TableProps } from 'antd';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { archiveArticle, getArticles, restoreArticle } from '../../api/article';
import { ArticleTypeTag, StatusTag } from '../../components/ArticleMetaTag';
import PageContainer from '../../components/PageContainer';
import SectionCard from '../../components/SectionCard';
import type { ArticleListItem, ArticleQuery } from '../../types/article';
import {
  articleStatusOptions,
  getArticleStatusLabel,
} from '../../types/article';
import { formatDateTime } from '../../utils/datetime';
import { formatFailure } from '../../utils/feedback';

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
        try {
          await archiveArticle(article.id);
          message.success('归档成功');
          await loadArticles();
        } catch (error) {
          message.error(formatFailure('归档', error));
        }
      },
    });
  };

  const handleRestore = async (article: ArticleListItem) => {
    try {
      await restoreArticle(article.id);
      message.success('恢复成功');
      await loadArticles();
    } catch (error) {
      message.error(formatFailure('恢复', error));
    }
  };

  const formatLatestTime = (record: ArticleListItem) => {
    const value = record.updatedAt || record.createdAt;
    return formatDateTime(value);
  };

  const renderActions = (record: ArticleListItem) => {
    if (record.status === 'ARCHIVED') {
      return (
        <Space size={8} wrap={false}>
          <Button size="small" type="primary" onClick={() => navigate(`/articles/${record.id}`)}>
            查看/编辑
          </Button>
          <Button size="small" onClick={() => void handleRestore(record)}>
            恢复
          </Button>
        </Space>
      );
    }

    return (
      <Space size={8} wrap={false}>
        <Button size="small" type="primary" onClick={() => navigate(`/articles/${record.id}`)}>
          查看/编辑
        </Button>
        <Button size="small" onClick={() => navigate(`/articles/${record.id}?openPlatformGenerate=1`)}>
          生成平台稿
        </Button>
        <Button size="small" onClick={() => navigate(`/publish?articleId=${record.id}`)}>
          发布平台稿
        </Button>
        <Button size="small" danger onClick={() => handleArchive(record)}>
          归档
        </Button>
      </Space>
    );
  };

  const columns: TableProps<ArticleListItem>['columns'] = [
    {
      title: '标题',
      dataIndex: 'title',
      width: '30%',
      render: (value, record) => (
        <Button type="link" className="table-link-button" onClick={() => navigate(`/articles/${record.id}`)}>
          {value}
        </Button>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: '9%',
      render: (status) => <StatusTag status={status} />,
    },
    {
      title: '文章类型',
      dataIndex: 'type',
      width: '12%',
      render: (type) => <ArticleTypeTag type={type} />,
    },
    {
      title: '完成时间',
      key: 'latestTime',
      width: '17%',
      render: (_, record) => (
        <Typography.Text style={{ whiteSpace: 'nowrap' }}>
          {formatLatestTime(record)}
        </Typography.Text>
      ),
    },
    {
      title: '操作',
      key: 'actions',
      fixed: 'right',
      width: '32%',
      render: (_, record) => renderActions(record),
    },
  ];

  return (
    <PageContainer
      title="文章库"
      description="所有文章都在这里沉淀：快速查找、编辑和归档，也可以进入详情继续生成平台稿。"
    >
      <SectionCard className="article-list-card">
        <Space direction="vertical" size={18} style={{ width: '100%' }}>
          <Space style={{ width: '100%', justifyContent: 'space-between' }}>
            <Typography.Text strong>文章列表</Typography.Text>
            <Space>
              <Button type="primary" onClick={() => navigate('/ai-generate')}>
                AI生成文章
              </Button>
              <Button onClick={() => navigate('/articles/new')}>
                手动新建文章
              </Button>
            </Space>
          </Space>
          <Form form={form} layout="inline" onFinish={() => void loadArticles(1, pagination.pageSize)}>
            <Form.Item name="keyword">
              <Input.Search placeholder="搜索标题、标签或关键词" allowClear onSearch={() => void loadArticles(1, pagination.pageSize)} />
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
            className="article-library-table"
            tableLayout="fixed"
            rowKey="id"
            loading={loading}
            columns={columns}
            dataSource={articles}
            locale={{
              emptyText: (
                <Empty description="暂无文章">
                  <Button type="primary" onClick={() => navigate('/ai-generate')}>
                    去生成文章
                  </Button>
                </Empty>
              ),
            }}
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
