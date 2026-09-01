output "kubeconfig_path" {
  description = "Caminho do kubeconfig gerado."
  value       = var.kubeconfig_path
}

output "apiserver" {
  description = "Endereço do apiserver no kubeconfig."
  value       = "https://${var.apiserver_host}:16443"
}
