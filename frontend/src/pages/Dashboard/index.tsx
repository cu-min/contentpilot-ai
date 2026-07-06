import { Alert, Button, Col, Row, Space, Spin, Statistic, Typography } from 'antd';
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
  { title: '平台账号', description: '配置掘金等平台账号', path: '/platform' },
  { title: '发布任务', description: '创建任务并提交待执行', path: '/publish' },
  { title: '执行发布', description: 'Mock 或掘金草稿更新', path: '/publish' },
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

  return (
    <PageContainer
      title="首页概览"
      description="聚焦当前 MVP 主链路：从产品配置、AI 文章生成、多平台适配，到发布任务执行与掘金草稿更新。"
    >
      <SectionCard className="dashboard-panel">
        <div className="dashboard-status">
          <div>
            <Typography.Text className="dashboard-eyebrow">MVP 工作台</Typography.Text>
            <Typography.Title level={4} className="dashboard-heading">
              内容生成到发布执行闭环
            </Typography.Title>
            <Typography.Paragraph className="dashboard-copy">
              从产品信息到平台发布稿，再到发布任务执行，运营人员可以按流程完成一次内容分发准备。
            </Typography.Paragraph>
          </div>
          <Space wrap>
            <Button type="primary" onClick={() => navigate('/ai-generate')}>生成文章</Button>
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
              <Statistic title="真实平台试点" value="掘金草稿" />
            </div>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <div className="dashboard-stat">
              <Statistic title="发布模式" value="手动执行" />
            </div>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <div className="dashboard-stat">
              <Statistic title="后端状态" value={backendStatus} />
            </div>
          </Col>
        </Row>

        <div className="dashboard-health">
          {loading && (
            <Space>
              <Spin size="small" />
              <Typography.Text type="secondary">正在检查后端服务</Typography.Text>
            </Space>
          )}
          {!loading && healthMessage && (
            <Alert type="success" message={healthMessage} showIcon />
          )}
          {!loading && error && <Alert type="error" message={error} showIcon />}
        </div>
      </SectionCard>

      <SectionCard className="dashboard-panel">
        <div className="dashboard-section-head">
          <div>
            <Typography.Title level={5}>MVP 主流程</Typography.Title>
            <Typography.Text type="secondary">按顺序完成即可得到一条可演示的内容营销发布链路。</Typography.Text>
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
