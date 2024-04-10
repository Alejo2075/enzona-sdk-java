package com.desoft.enzonasdk.model.request;

import lombok.Data;

@Data
public class CreateClaimsRequest {
    private String typeClaim;
    private String V_tipo_reclamacion;
    private String V_motivo;
    private String no_operacionEZ;
    private String telefono_cliente_afec;
    private String correo_cliente_afec;
    private String cliente_afectado;
    private String id_op_comercio;
    private String nombre_comercio;
    private String id_cliente;
    private String oficina_comercial;
    private String no_factura;
}
