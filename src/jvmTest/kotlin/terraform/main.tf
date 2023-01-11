
provider "aws" {
  alias  = "primary"
  region = var.primary_aws_region
}

resource "aws_dynamodb_table" "assignment_schedule" {

  name     = "priority_queue_table"
  billing_mode = "PAY_PER_REQUEST"
  hash_key = "id"

  attribute {
    name = "id"
    type = "S"
  }

  attribute {
    name = "schedule"
    type = "S"
  }

  attribute {
    name = "queued"
    type = "N"
  }

  global_secondary_index {
    name               = "scheduled-index"
    hash_key           = "queued"
    range_key          = "schedule"
    projection_type    = "ALL"
  }

}

