import { Button, Col, Row, Space, Spin, Statistic, Typography } from 'antd';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getHealth } from '../../api/health';
import PageContainer from '../../components/PageContainer';
import SectionCard from '../../components/SectionCard';
import './style.css';

const workflowSteps = [
  { title: '产品配置', description: '维护产品信息、卖点和禁用词', path: '/product' },
  { title: 'AI 生成文章', description: '基于主题生成营销内容', path: '/ai-generate' },
  { title: '文章库', description: '编辑正文并管理文章状态', path: '/articles' },
  { title: '平台稿', description: '为不同平台重组内容', path: '/articles' },
  { title: '平台账号', description: '配置各平台发布账号', path: '/platform' },
  { title: '发布任务', description: '创建任务并准备平台草稿', path: '/publish' },
  { title: '人工发布', description: '检查草稿并在平台确认发布', path: '/publish' },
];

export default function Dashboard() {
  const navigate = useNavigate();
  const [healthMessage, setHealthMessage] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    getHealth()
      .then((result) => {
        setHealthMessage(result.data);
      })
      .catch(() => {
        setError('后端健康检查暂不可用');
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

  const backendStatus = healthMessage ? '运行中' : error ? '异常' : '检测中';
  const backendStatusClass = healthMessage ? 'is-success' : error ? 'is-error' : 'is-loading';

  return (
    <PageContainer
      title="首页概览"
      description="从一个想法到多平台发布，让好内容更快成形，让好产品被更多人看见。"
    >
      <SectionCard className="dashboard-panel">
        <div className="dashboard-status">
          <div>
            <Typography.Text className="dashboard-eyebrow">内容营销工作台</Typography.Text>
            <Typography.Title level={4} className="dashboard-heading">
              从选题生成到全平台发布的一体化流程
            </Typography.Title>
            <Typography.Paragraph className="dashboard-copy">
              运营人员可以从产品资料生成文章，适配微信公众号、掘金、CSDN、知乎等平台，由系统准备草稿后人工检查发布。
            </Typography.Paragraph>
          </div>
          <Space wrap>
            <Button type="primary" onClick={() => navigate('/ai-generate')}>AI生成文章</Button>
            <Button onClick={() => navigate('/publish')}>查看发布任务</Button>
          </Space>
        </div>

        <Row gutter={[16, 16]} className="dashboard-stat-grid">
          <Col xs={24} sm={12} lg={6}>
            <div className="dashboard-stat">
              <Statistic title="主链路" value="已打通" />
            </div>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <div className="dashboard-stat">
              <Statistic title="适配平台" value="多平台" />
            </div>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <div className="dashboard-stat">
              <Statistic title="发布模式" value="人工确认" />
            </div>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <div className="dashboard-stat">
              <Statistic title="发布链路" value="准备到发布前" />
            </div>
          </Col>
        </Row>

        <div className={`dashboard-health-card ${backendStatusClass}`}>
          <div className="dashboard-health-indicator" />
          <div>
            <Typography.Text className="dashboard-health-title">后端服务</Typography.Text>
            <Typography.Title level={5} className="dashboard-health-status">
              {backendStatus}
            </Typography.Title>
            <Typography.Text className="dashboard-health-detail">
              {loading ? '正在检查服务连接' : healthMessage || error}
            </Typography.Text>
          </div>
          {loading && (
            <Space className="dashboard-health-spin">
              <Spin size="small" />
            </Space>
          )}
        </div>
      </SectionCard>

      <SectionCard className="dashboard-panel">
        <div className="dashboard-section-head">
          <div>
            <Typography.Title level={5}>主流程</Typography.Title>
            <Typography.Text type="secondary">按顺序准备平台内容，最终发布动作由运营人员在平台页面检查并确认。</Typography.Text>
          </div>
        </div>
        <div className="workflow-grid">
          {workflowSteps.map((step, index) => (
            <button
              className="workflow-item"
              key={step.title}
              type="button"
              onClick={() => navigate(step.path)}
            >
              <span className="workflow-index">{index + 1}</span>
              <span className="workflow-title">{step.title}</span>
              <span className="workflow-desc">{step.description}</span>
            </button>
          ))}
        </div>
      </SectionCard>
    </PageContainer>
  );
}
