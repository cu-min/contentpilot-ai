import './style.css';

interface MarkdownPreviewProps {
  value?: string;
}

function escapeHtml(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

function renderInline(value: string) {
  return escapeHtml(value)
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/`(.+?)`/g, '<code>$1</code>');
}

function renderMarkdown(value: string) {
  const lines = value.split('\n');
  let inCodeBlock = false;
  const html: string[] = [];

  lines.forEach((line) => {
    if (line.trim().startsWith('```')) {
      if (inCodeBlock) {
        html.push('</code></pre>');
      } else {
        html.push('<pre><code>');
      }
      inCodeBlock = !inCodeBlock;
      return;
    }

    if (inCodeBlock) {
      html.push(`${escapeHtml(line)}\n`);
      return;
    }

    if (!line.trim()) {
      html.push('<br />');
      return;
    }

    if (line.startsWith('### ')) {
      html.push(`<h3>${renderInline(line.slice(4))}</h3>`);
      return;
    }
    if (line.startsWith('## ')) {
      html.push(`<h2>${renderInline(line.slice(3))}</h2>`);
      return;
    }
    if (line.startsWith('# ')) {
      html.push(`<h1>${renderInline(line.slice(2))}</h1>`);
      return;
    }
    if (line.startsWith('- ')) {
      html.push(`<p class="markdown-list-item">${renderInline(line.slice(2))}</p>`);
      return;
    }

    html.push(`<p>${renderInline(line)}</p>`);
  });

  if (inCodeBlock) {
    html.push('</code></pre>');
  }

  return html.join('');
}

export default function MarkdownPreview({ value }: MarkdownPreviewProps) {
  const html = value ? renderMarkdown(value) : '<p class="markdown-empty">暂无正文预览</p>';

  return (
    <div
      className="markdown-preview"
      dangerouslySetInnerHTML={{ __html: html }}
    />
  );
}
