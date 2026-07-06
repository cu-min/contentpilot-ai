import {
  Button,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import type { TableProps } from 'antd';
import { useEffect, useState } from 'react';
import { createUser, getUsers, updateUserStatus } from '../../api/user';
import PageContainer from '../../components/PageContainer';
import SectionCard from '../../components/SectionCard';
import type { UserInfo } from '../../types';
import { formatFailure } from '../../utils/feedback';

interface CreateUserFormValues {
  username: string;
  password: string;
  nickname?: string;
  email?: string;
  role: 'ADMIN' | 'OPERATOR';
}

export default function User() {
  const [users, setUsers] = useState<UserInfo[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });
  const [form] = Form.useForm<CreateUserFormValues>();

  const loadUsers = async (page = pagination.current, size = pagination.pageSize) => {
    setLoading(true);
    try {
      const result = await getUsers({ page, size });
      setUsers(result.data.records);
      setPagination({
        current: result.data.current,
        pageSize: result.data.size,
        total: result.data.total,
      });
    } catch (error) {
      message.error(error instanceof Error ? error.message : '用户列表加载失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadUsers(1, 10);
  }, []);

  const handleCreate = async () => {
    const values = await form.validateFields();
    setSubmitting(true);
    try {
      await createUser(values);
      message.success('保存成功');
      setModalOpen(false);
      form.resetFields();
      await loadUsers(1, pagination.pageSize);
    } catch (error) {
      message.error(formatFailure('保存', error));
    } finally {
      setSubmitting(false);
    }
  };

  const handleStatusChange = async (user: UserInfo, checked: boolean) => {
    try {
      await updateUserStatus(user.id, checked ? 1 : 0);
      message.success(checked ? '启用成功' : '禁用成功');
      await loadUsers();
    } catch (error) {
      message.error(formatFailure(checked ? '启用' : '禁用', error));
    }
  };

  const columns: TableProps<UserInfo>['columns'] = [
    { title: '用户名', dataIndex: 'username', width: 160 },
    { title: '昵称', dataIndex: 'nickname', width: 160, render: (value) => value || '-' },
    { title: '邮箱', dataIndex: 'email', width: 240, render: (value) => value || '-' },
    {
      title: '角色',
      dataIndex: 'role',
      width: 120,
      render: (role) => (
        <Tag color={role === 'ADMIN' ? 'blue' : 'green'}>
          {role === 'ADMIN' ? '管理员' : '运营人员'}
        </Tag>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 110,
      render: (status) => (
        <Tag color={status === 1 ? 'success' : 'default'}>
          {status === 1 ? '启用' : '禁用'}
        </Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 140,
      render: (_, record) => (
        <Switch
          checkedChildren="启用"
          unCheckedChildren="禁用"
          checked={record.status === 1}
          onChange={(checked) => void handleStatusChange(record, checked)}
        />
      ),
    },
  ];

  return (
    <PageContainer
      title="用户管理"
      description="管理系统用户、角色和启用状态。"
    >
      <SectionCard>
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <Space style={{ width: '100%', justifyContent: 'space-between' }}>
            <Typography.Text strong>用户列表</Typography.Text>
            <Button type="primary" onClick={() => setModalOpen(true)}>
              新增用户
            </Button>
          </Space>
          <Table
            rowKey="id"
            loading={loading}
            columns={columns}
            dataSource={users}
            pagination={{
              current: pagination.current,
              pageSize: pagination.pageSize,
              total: pagination.total,
              onChange: (page, pageSize) => void loadUsers(page, pageSize),
            }}
          />
        </Space>
      </SectionCard>
      <Modal
        title="新增用户"
        open={modalOpen}
        confirmLoading={submitting}
        onOk={() => void handleCreate()}
        onCancel={() => setModalOpen(false)}
        destroyOnClose
      >
        <Form
          form={form}
          layout="vertical"
          initialValues={{ role: 'OPERATOR' }}
          preserve={false}
        >
          <Form.Item
            label="用户名"
            name="username"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input placeholder="例如 operator" />
          </Form.Item>
          <Form.Item
            label="密码"
            name="password"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password placeholder="请输入初始密码" />
          </Form.Item>
          <Form.Item label="昵称" name="nickname">
            <Input placeholder="请输入昵称" />
          </Form.Item>
          <Form.Item label="邮箱" name="email" rules={[{ type: 'email', message: '邮箱格式不正确' }]}>
            <Input placeholder="operator@example.com" />
          </Form.Item>
          <Form.Item
            label="角色"
            name="role"
            rules={[{ required: true, message: '请选择角色' }]}
          >
            <Select
              options={[
                { label: '运营人员', value: 'OPERATOR' },
                { label: '管理员', value: 'ADMIN' },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>
    </PageContainer>
  );
}
