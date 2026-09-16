package com.telecom.tollfree.domain;
import jakarta.persistence.*; import java.time.Instant;
@Entity @Table(name="audit_logs",indexes=@Index(name="ix_audit_number",columnList="number")) public class AuditLog { @Id @GeneratedValue(strategy=GenerationType.UUID) private String id; @Column(nullable=false) private String number; @Column(nullable=false) private String action; @Column(nullable=false) private Instant createdAt=Instant.now(); protected AuditLog(){} public AuditLog(String n,String a){number=n;action=a;} }
