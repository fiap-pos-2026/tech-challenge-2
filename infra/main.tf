# Recursos de apoio (namespace, PostgreSQL, Redis, Mailpit) sobre o cluster provisionado em
# cluster.tf. Sem Helm charts de terceiros: manifests equivalentes via provider kubernetes,
# usando as mesmas imagens do docker-compose.yml para paridade dev/prd e nomes de Service que
# casam com JDBC_HOST/REDIS_HOST já esperados por k8s/base/configmap.yaml (`postgres`, `redis`).

resource "kubernetes_namespace" "tech_challenge" {
  # Garante que o cluster (microk8s instalado/habilitado em cluster.tf, quando
  # manage_microk8s = true) já existe antes de qualquer recurso kubernetes_* ser aplicado.
  depends_on = [null_resource.microk8s]

  metadata {
    name = var.namespace
  }
}

resource "kubernetes_secret" "postgres" {
  metadata {
    name      = "postgres-credentials"
    namespace = kubernetes_namespace.tech_challenge.metadata[0].name
  }

  data = {
    POSTGRES_DB       = var.db_name
    POSTGRES_USER     = var.db_username
    POSTGRES_PASSWORD = var.db_password
  }

  type = "Opaque"
}

resource "kubernetes_stateful_set" "postgres" {
  metadata {
    name      = "postgres"
    namespace = kubernetes_namespace.tech_challenge.metadata[0].name
    labels = {
      "app.kubernetes.io/name" = "postgres"
    }
  }

  spec {
    service_name = "postgres"
    replicas     = 1

    selector {
      match_labels = {
        "app.kubernetes.io/name" = "postgres"
      }
    }

    template {
      metadata {
        labels = {
          "app.kubernetes.io/name" = "postgres"
        }
      }

      spec {
        container {
          name  = "postgres"
          image = var.postgres_image

          env_from {
            secret_ref {
              name = kubernetes_secret.postgres.metadata[0].name
            }
          }

          port {
            name           = "postgres"
            container_port = 5432
          }

          volume_mount {
            name       = "postgres-data"
            mount_path = "/var/lib/postgresql"
          }

          readiness_probe {
            exec {
              command = ["pg_isready", "-U", var.db_username, "-d", var.db_name]
            }
            initial_delay_seconds = 5
            period_seconds        = 10
          }

          liveness_probe {
            exec {
              command = ["pg_isready", "-U", var.db_username, "-d", var.db_name]
            }
            initial_delay_seconds = 15
            period_seconds        = 20
          }
        }
      }
    }

    volume_claim_template {
      metadata {
        name = "postgres-data"
      }
      spec {
        access_modes = ["ReadWriteOnce"]
        resources {
          requests = {
            storage = var.postgres_storage_size
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "postgres" {
  metadata {
    name      = "postgres"
    namespace = kubernetes_namespace.tech_challenge.metadata[0].name
  }

  spec {
    selector = {
      "app.kubernetes.io/name" = "postgres"
    }

    port {
      name        = "postgres"
      port        = 5432
      target_port = "postgres"
    }

    type = "ClusterIP"
  }
}

# Redis sem senha — mesmo comportamento do docker-compose.yml (`--save ""`, sem auth);
# a app hoje não configura spring.data.redis.password.
resource "kubernetes_deployment" "redis" {
  metadata {
    name      = "redis"
    namespace = kubernetes_namespace.tech_challenge.metadata[0].name
    labels = {
      "app.kubernetes.io/name" = "redis"
    }
  }

  spec {
    replicas = 1

    selector {
      match_labels = {
        "app.kubernetes.io/name" = "redis"
      }
    }

    template {
      metadata {
        labels = {
          "app.kubernetes.io/name" = "redis"
        }
      }

      spec {
        container {
          name    = "redis"
          image   = var.redis_image
          command = ["redis-server", "--save", ""]

          port {
            name           = "redis"
            container_port = 6379
          }

          readiness_probe {
            exec {
              command = ["redis-cli", "ping"]
            }
            initial_delay_seconds = 5
            period_seconds        = 10
          }

          liveness_probe {
            exec {
              command = ["redis-cli", "ping"]
            }
            initial_delay_seconds = 10
            period_seconds        = 20
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "redis" {
  metadata {
    name      = "redis"
    namespace = kubernetes_namespace.tech_challenge.metadata[0].name
  }

  spec {
    selector = {
      "app.kubernetes.io/name" = "redis"
    }

    port {
      name        = "redis"
      port        = 6379
      target_port = "redis"
    }

    type = "ClusterIP"
  }
}

# Mailpit captura os e-mails de OTP e de mudança de status no ambiente local,
# mantendo o mesmo serviço usado pelo docker-compose.yml.
resource "kubernetes_deployment" "mailpit" {
  metadata {
    name      = "mailpit"
    namespace = kubernetes_namespace.tech_challenge.metadata[0].name
    labels = {
      "app.kubernetes.io/name" = "mailpit"
    }
  }

  spec {
    replicas = 1

    selector {
      match_labels = {
        "app.kubernetes.io/name" = "mailpit"
      }
    }

    template {
      metadata {
        labels = {
          "app.kubernetes.io/name" = "mailpit"
        }
      }

      spec {
        container {
          name  = "mailpit"
          image = "axllent/mailpit:latest"

          port {
            name           = "smtp"
            container_port = 1025
          }

          port {
            name           = "web"
            container_port = 8025
          }

          readiness_probe {
            http_get {
              path = "/"
              port = "web"
            }
            initial_delay_seconds = 5
            period_seconds        = 10
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "mailpit" {
  metadata {
    name      = "mailpit"
    namespace = kubernetes_namespace.tech_challenge.metadata[0].name
  }

  spec {
    selector = {
      "app.kubernetes.io/name" = "mailpit"
    }

    port {
      name        = "smtp"
      port        = 1025
      target_port = "smtp"
    }

    port {
      name        = "web"
      port        = 8025
      target_port = "web"
    }

    type = "ClusterIP"
  }
}
