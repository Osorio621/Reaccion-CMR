-- Seed inicial de catálogos (idempotente: ON CONFLICT no duplica)
INSERT INTO public.catalogos (tipo, codigo, nombre, descripcion, probabilidad_default, orden, activo, created_at, updated_at)
VALUES
    ('ETAPA_PIPELINE', 'ETAPA_PROSPECCION',      'Prospección',      'Identificación de clientes potenciales',            10, 1, true, now(), now()),
    ('ETAPA_PIPELINE', 'ETAPA_CALIFICACION',     'Calificación',     'Evaluación del interés y presupuesto',               25, 2, true, now(), now()),
    ('ETAPA_PIPELINE', 'ETAPA_PROPUESTA',        'Propuesta',        'Cliente recibió propuesta comercial',                50, 3, true, now(), now()),
    ('ETAPA_PIPELINE', 'ETAPA_NEGOCIACION',      'Negociación',      'Negociación de términos y condiciones',              75, 4, true, now(), now()),
    ('ETAPA_PIPELINE', 'ETAPA_CIERRE_GANADO',    'Cierre Ganado',    'Oportunidad cerrada con éxito',                     100, 5, true, now(), now()),
    ('ETAPA_PIPELINE', 'ETAPA_CIERRE_PERDIDO',   'Cierre Perdido',   'Oportunidad cerrada sin éxito',                       0, 6, true, now(), now()),

    ('ETAPA_PROSPECTO', 'PROSPECTO_NUEVO',        'Nuevo',            'Prospecto recién registrado',                      NULL, 1, true, now(), now()),
    ('ETAPA_PROSPECTO', 'PROSPECTO_EN_CONTACTO',  'En Contacto',      'Se estableció comunicación',                        NULL, 2, true, now(), now()),
    ('ETAPA_PROSPECTO', 'PROSPECTO_CALIFICADO',   'Calificado',       'Cumple criterios de venta',                         NULL, 3, true, now(), now()),
    ('ETAPA_PROSPECTO', 'PROSPECTO_CONVERTIDO',   'Convertido',       'Se convirtió en cliente',                           NULL, 4, true, now(), now()),
    ('ETAPA_PROSPECTO', 'PROSPECTO_DESCARTADO',   'Descartado',       'No es viable comercialmente',                       NULL, 5, true, now(), now()),

    ('TIPO_CLIENTE', 'CLIENTE_LABORATORIO',      'Laboratorio',      'Laboratorio de análisis clínico',                   NULL, 1, true, now(), now()),
    ('TIPO_CLIENTE', 'CLIENTE_HOSPITAL',         'Hospital',         'Institución hospitalaria',                          NULL, 2, true, now(), now()),
    ('TIPO_CLIENTE', 'CLIENTE_CLINICA',          'Clínica',          'Clínica o centro de salud',                         NULL, 3, true, now(), now()),
    ('TIPO_CLIENTE', 'CLIENTE_FARMACIA',         'Farmacia',         'Cadena o farmacia independiente',                   NULL, 4, true, now(), now()),
    ('TIPO_CLIENTE', 'CLIENTE_UNIVERSIDAD',      'Universidad',      'Institución educativa',                             NULL, 5, true, now(), now()),
    ('TIPO_CLIENTE', 'CLIENTE_GOBIERNO',         'Gobierno',         'Entidad gubernamental',                             NULL, 6, true, now(), now()),

    ('INDUSTRIA', 'INDUSTRIA_SALUD',             'Salud',            'Sector salud',                                      NULL, 1, true, now(), now()),
    ('INDUSTRIA', 'INDUSTRIA_EDUCACION',         'Educación',        'Sector educativo',                                  NULL, 2, true, now(), now()),
    ('INDUSTRIA', 'INDUSTRIA_INDUSTRIA',         'Industria',        'Sector industrial',                                 NULL, 3, true, now(), now()),
    ('INDUSTRIA', 'INDUSTRIA_GOBIERNO',          'Gobierno',         'Sector gubernamental',                              NULL, 4, true, now(), now()),
    ('INDUSTRIA', 'INDUSTRIA_INVESTIGACION',     'Investigación',    'Centros de investigación',                          NULL, 5, true, now(), now()),

    ('ZONA_GEOGRAFICA', 'ZONA_NORTE',            'Norte',            'Zona norte',                                        NULL, 1, true, now(), now()),
    ('ZONA_GEOGRAFICA', 'ZONA_CENTRO',           'Centro',           'Zona centro',                                       NULL, 2, true, now(), now()),
    ('ZONA_GEOGRAFICA', 'ZONA_SUR',              'Sur',              'Zona sur',                                          NULL, 3, true, now(), now()),
    ('ZONA_GEOGRAFICA', 'ZONA_METROPOLITANA',    'Zona Metropolitana', 'Zona metropolitana',                              NULL, 4, true, now(), now()),

    ('RESULTADO_VISITA', 'VISITA_EXITOSA',       'Exitosa',          'Visita con resultado positivo',                     NULL, 1, true, now(), now()),
    ('RESULTADO_VISITA', 'VISITA_SEGUIMIENTO',   'Seguimiento requerido', 'Se requiere nueva visita o seguimiento',      NULL, 2, true, now(), now()),
    ('RESULTADO_VISITA', 'VISITA_SIN_INTERES',   'Sin interés',      'Cliente no mostró interés',                         NULL, 3, true, now(), now()),
    ('RESULTADO_VISITA', 'VISITA_PROPUESTA',     'Propuesta solicitada', 'Cliente solicitó propuesta',                  NULL, 4, true, now(), now()),

    ('TIPO_SEGUIMIENTO', 'SEGUIMIENTO_LLAMADA',  'Llamada',          'Llamada telefónica',                                NULL, 1, true, now(), now()),
    ('TIPO_SEGUIMIENTO', 'SEGUIMIENTO_EMAIL',    'Email',            'Correo electrónico',                                NULL, 2, true, now(), now()),
    ('TIPO_SEGUIMIENTO', 'SEGUIMIENTO_REUNION',  'Reunión',          'Reunión presencial o virtual',                      NULL, 3, true, now(), now()),
    ('TIPO_SEGUIMIENTO', 'SEGUIMIENTO_DEMO',     'Demo',             'Demostración de producto',                          NULL, 4, true, now(), now()),
    ('TIPO_SEGUIMIENTO', 'SEGUIMIENTO_PROPUESTA','Propuesta',        'Entrega de propuesta',                              NULL, 5, true, now(), now()),
    ('TIPO_SEGUIMIENTO', 'SEGUIMIENTO_VISITA',   'Visita',           'Visita a cliente',                                  NULL, 6, true, now(), now())
ON CONFLICT (codigo) DO NOTHING;