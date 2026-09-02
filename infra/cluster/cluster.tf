resource "null_resource" "microk8s" {
  triggers = {
    always_run     = timestamp()
    addons         = join(",", var.microk8s_addons)
    channel        = var.microk8s_channel
    apiserver_host = var.apiserver_host
  }

  provisioner "local-exec" {
    command = "bash ${path.module}/scripts/setup-microk8s.sh"
    environment = {
      KUBECONFIG_PATH   = pathexpand(var.kubeconfig_path)
      APISERVER_HOST    = var.apiserver_host
      MICROK8S_CHANNEL  = var.microk8s_channel
      MICROK8S_ADDONS   = join(" ", var.microk8s_addons)
    }
  }
}
