-- Crear esquema
CREATE SCHEMA IF NOT EXISTS inbox_messages;

-- Tabla de mensajes
CREATE TABLE IF NOT EXISTS inbox_messages.messages (
    id_message SERIAL PRIMARY KEY,
    sender_id INTEGER NOT NULL,
    content TEXT,
    create_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    asunto TEXT
);

-- Tabla de destinatarios de mensajes
CREATE TABLE IF NOT EXISTS inbox_messages.message_recipients (
    id SERIAL PRIMARY KEY,
    message_id INTEGER NOT NULL,
    recipient_id INTEGER NOT NULL,
    is_read VARCHAR(1) DEFAULT 'N' CHECK (is_read IN ('Y', 'N')),
    read_at TIMESTAMP,
    CONSTRAINT fk_message_recipients_message
        FOREIGN KEY (message_id)
        REFERENCES inbox_messages.messages(id_message)
        ON DELETE CASCADE
);

-- Tabla de adjuntos
CREATE TABLE IF NOT EXISTS inbox_messages.attachments (
    id SERIAL PRIMARY KEY,
    message_id INTEGER NOT NULL,
    file_path VARCHAR(255) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(255),
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_attachments_message
        FOREIGN KEY (message_id)
        REFERENCES inbox_messages.messages(id_message)
        ON DELETE CASCADE
);

-- Índices para mejorar performance
CREATE INDEX idx_message_recipients_message_id ON inbox_messages.message_recipients(message_id);
CREATE INDEX idx_message_recipients_recipient_id ON inbox_messages.message_recipients(recipient_id);
CREATE INDEX idx_message_recipients_is_read ON inbox_messages.message_recipients(is_read);
CREATE INDEX idx_attachments_message_id ON inbox_messages.attachments(message_id);
CREATE INDEX idx_messages_sender_id ON inbox_messages.messages(sender_id);
CREATE INDEX idx_messages_create_at ON inbox_messages.messages(create_at);

-- Comentarios para documentación
COMMENT ON TABLE inbox_messages.messages IS 'Almacena los mensajes enviados en el sistema';
COMMENT ON TABLE inbox_messages.message_recipients IS 'Gestiona los destinatarios de cada mensaje y su estado de lectura';
COMMENT ON TABLE inbox_messages.attachments IS 'Almacena información de archivos adjuntos a los mensajes';
