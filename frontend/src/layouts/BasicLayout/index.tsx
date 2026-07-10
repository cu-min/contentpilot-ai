import { Button, Layout, Menu, Space, Tag, Typography } from 'antd';
import type { MenuProps } from 'antd';
import { observer } from 'mobx-react-lite';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { authStore } from '../../stores';
import './style.css';

const { Header, Content, Sider } = Layout;

const baseMenuItems: NonNullable<MenuProps['items']> = [
  { key: '/dashboard', label: '首页概览' },
  { key: '/articles', label: '文章库' },
  { key: '/product', label: '产品配置' },
  { key: '/platform', label: '平台管理' },
  { key: '/publish', label: '发布任务' },
  {
    key: '/growth',
    label: '内容增长',
    children: [{ key: '/growth/tracking', label: '跟踪查询' }],
  },
];

function BasicLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const selectedMenuKey = location.pathname.startsWith('/articles') || location.pathname.startsWith('/ai-generate')
    ? '/articles'
    : location.pathname.startsWith('/growth')
      ? '/growth/tracking'
    : location.pathname;

  const menuItems = authStore.role === 'ADMIN'
    ? [
        ...baseMenuItems,
        { key: '/user', label: '用户管理' },
      ]
    : baseMenuItems;

  const handleMenuClick: MenuProps['onClick'] = ({ key }) => {
    navigate(key);
  };

  const handleLogout = () => {
    authStore.clearAuth();
    navigate('/login', { replace: true });
  };

  return (
    <Layout className="basic-layout">
      <Sider className="basic-sider" width={224}>
        <div className="brand">AI内容营销系统</div>
        <Menu
          className="side-menu"
          mode="inline"
          selectedKeys={[selectedMenuKey]}
          defaultOpenKeys={['/growth']}
          items={menuItems}
          onClick={handleMenuClick}
        />
      </Sider>
      <Layout className="basic-main">
        <Header className="top-header">
          <Typography.Text strong>让好内容自己生长，让好产品被更多人看见。</Typography.Text>
          <Space size={16} align="center">
            <Tag color={authStore.role === 'ADMIN' ? 'blue' : 'green'}>
              {authStore.role === 'ADMIN' ? '管理员' : '运营人员'}
            </Tag>
            <Button onClick={handleLogout}>退出登录</Button>
          </Space>
        </Header>
        <Content className="main-content">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}

export default observer(BasicLayout);
