import {
  Alert,
  Button,
  Empty,
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
import type { TableColumnsType } from 'antd';
import { useEffect, useState } from 'react';
import {
  createPlatformAccount,
  getPlatformAccountDetail,
  getPlatformAccounts,
  updatePlatformAccount,
  updatePlatformAccountStatus,
} from '../../api/platformAccount';
import PageContainer from '../../components/PageContainer';
import SectionCard from '../../components/SectionCard';
import type { PlatformAccount, PlatformAccountPayload } from '../../types/platformAccount';
import {
  authTypeOptions,
  getAuthTypeLabel,
  getPublishModeLabel,
  publishModeOptions,
} from '../../types/platformAccount';
import {
  getPlatformContentPlatformLabel,
  platformContentOptions,
} from '../../types/platformContent';
import { formatFailure } from '../../utils/feedback';

const { TextArea } = Input;

export default function Platform() {
  const [form] = Form.useForm<PlatformAccountPayload>();
  const [accounts, setAccounts] = useState<PlatformAccount[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<PlatformAccount | null>(null);

  const loadAccounts = async () => {
    setLoading(true);
    try {
      const result = await getPlatformAccounts();
      setAccounts(result.data);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '平台账号加载失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadAccounts();
  }, []);

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({
      platform: 'WECHAT_OFFICIAL',
      authType: 'APP_SECRET',
      defaultPublishMode: 'OFFICIAL_API',
      enabled: 1,
    });
    setModalOpen(true);
  };

  const openEdit = async (record: PlatformAccount) => {
    try {
      const result = await getPlatformAccountDetail(record.id);
      setEditing(result.data);
      form.setFieldsValue({
        ...result.data,
        authConfig: '',
      });
      setModalOpen(true);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '平台账号详情加载失败');
    }
  };

  const handleSave = async () => {
    const values = await form.validateFields();
    if (values.authConfig?.trim()) {
      try {
        JSON.parse(values.authConfig);
      } catch {
        const errorMessage = '认证配置必须是合法 JSON，请检查双引号、逗号和字段格式';
        form.setFields([{ name: 'authConfig', errors: [errorMessage] }]);
        message.error(`保存失败：${errorMessage}`);
        return;
      }
    }
    setSaving(true);
    try {
      if (editing) {
        await updatePlatformAccount(editing.id, values);
        message.success('保存成功');
      } else {
        await createPlatformAccount(values);
        message.success('保存成功');
      }
      setModalOpen(false);
      setEditing(null);
      form.resetFields();
      await loadAccounts();
    } catch (error) {
      message.error(formatFailure('保存', error));
    } finally {
      setSaving(false);
    }
  };

  const handleStatusChange = async (record: PlatformAccount, checked: boolean) => {
    try {
      await updatePlatformAccountStatus(record.id, checked ? 1 : 0);
      message.success(checked ? '启用成功' : '禁用成功');
      await loadAccounts();
    } catch (error) {
      message.error(formatFailure(checked ? '启用' : '禁用', error));
    }
  };

  const columns: TableColumnsType<PlatformAccount> = [
    {
      title: '平台',
      dataIndex: 'platform',
      width: 140,
      render: (platform: string) => (
        <Tag color="processing">{getPlatformContentPlatformLabel(platform)}</Tag>
      ),
    },
    {
      title: '账号名称',
      dataIndex: 'accountName',
      width: 220,
      ellipsis: true,
    },
    {
      title: '认证方式',
      dataIndex: 'authType',
      width: 150,
      responsive: ['lg'],
      render: (value: string) => getAuthTypeLabel(value),
    },
    {
      title: '认证配置',
      dataIndex: 'authConfigConfigured',
      width: 120,
      responsive: ['md'],
      render: (configured: boolean) => (
        <Tag color={configured ? 'warning' : 'default'}>
          {configured ? '已配置' : '未配置'}
        </Tag>
      ),
    },
    {
      title: '默认发布方式',
      dataIndex: 'defaultPublishMode',
      width: 160,
      render: (value: string) => <Tag>{getPublishModeLabel(value)}</Tag>,
    },
    {
      title: '启用',
      dataIndex: 'enabled',
      width: 100,
      render: (_, record) => (
        <Switch
          checked={record.enabled === 1}
          onChange={(checked) => handleStatusChange(record, checked)}
        />
      ),
    },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      width: 180,
      responsive: ['xl'],
      render: (value?: string) => value || '-',
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      render: (_, record) => (
        <Button size="small" onClick={() => openEdit(record)}>编辑</Button>
      ),
    },
  ];

  return (
    <PageContainer
      title="平台管理"
      description="配置公司统一平台账号，为后续不同平台 Publisher、草稿创建和自动填充流程预留认证与发布方式。"
    >
      <SectionCard>
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <Alert
            type="info"
            showIcon
            message="平台账号配置会在发布任务执行时使用。"
            description="认证配置属于敏感信息，列表和详情均不会明文回显；编辑时留空表示保留原配置。JUEJIN + UNOFFICIAL_API 会调用真实掘金接口。"
          />
          <Space style={{ width: '100%', justifyContent: 'space-between' }}>
            <Typography.Text strong>平台账号列表</Typography.Text>
            <Button type="primary" onClick={openCreate}>新增平台账号</Button>
          </Space>
          <Table
            rowKey="id"
            loading={loading}
            columns={columns}
            dataSource={accounts}
            pagination={false}
            locale={{
              emptyText: (
                <Empty description="暂无平台账号配置">
                  <Button type="primary" onClick={openCreate}>
                    新增平台账号
                  </Button>
                </Empty>
              ),
            }}
          />
        </Space>
      </SectionCard>

      <Modal
        title={editing ? '编辑平台账号' : '新增平台账号'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSave}
        okText="保存"
        cancelText="取消"
        confirmLoading={saving}
        width={720}
      >
        <Form form={form} layout="vertical" requiredMark="optional">
          <Form.Item
            label="平台"
            name="platform"
            rules={[{ required: true, message: '请选择平台' }]}
          >
            <Select options={[...platformContentOptions]} />
          </Form.Item>
          <Form.Item
            label="账号名称"
            name="accountName"
            rules={[{ required: true, message: '请输入账号名称' }]}
          >
            <Input maxLength={100} placeholder="例如：公司微信公众号主账号" />
          </Form.Item>
          <Form.Item
            label="认证方式"
            name="authType"
            rules={[{ required: true, message: '请选择认证方式' }]}
          >
            <Select options={[...authTypeOptions]} />
          </Form.Item>
          <Form.Item
            label="默认发布方式"
            name="defaultPublishMode"
            rules={[{ required: true, message: '请选择默认发布方式' }]}
          >
            <Select options={[...publishModeOptions]} />
          </Form.Item>
          <Form.Item
            label="认证配置"
            name="authConfig"
            extra={editing ? '编辑时留空表示保留原认证配置。掘金自动发布需要 cookie、defaultCategoryId、defaultTagIds，csrfToken 可选，敏感信息不要提交到代码仓库。' : '建议使用 JSON 字符串保存本地测试配置。掘金自动发布需要 cookie、defaultCategoryId、defaultTagIds，csrfToken 可选。'}
          >
            <TextArea
              rows={5}
              placeholder='例如：{"cookie":"本地填写","userAgent":"Mozilla/5.0 ...","csrfToken":"可选","aid":"2608","uuid":"","defaultCategoryId":"6809637776263217160","defaultTagIds":["6809640407484334093"],"draftOnly":false,"syncToOrg":false,"columnIds":[],"themeIds":[],"encryptedWordCount":1077883,"originWordCount":3}'
            />
          </Form.Item>
          <Form.Item label="启用状态" name="enabled" valuePropName="checked" getValueFromEvent={(checked) => (checked ? 1 : 0)} getValueProps={(value) => ({ checked: value === 1 })}>
            <Switch checkedChildren="启用" unCheckedChildren="禁用" />
          </Form.Item>
          <Form.Item label="备注" name="remark">
            <TextArea rows={3} maxLength={500} showCount placeholder="记录账号用途、草稿策略或人工确认要求" />
          </Form.Item>
        </Form>
      </Modal>
    </PageContainer>
  );
}
