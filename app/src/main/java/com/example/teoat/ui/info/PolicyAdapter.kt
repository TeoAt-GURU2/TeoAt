package com.example.teoat.ui.info

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.teoat.databinding.ItemPolicyRowBinding

class PolicyAdapter(
    private var policies : List<PolicyItem>,
    private val onDetailClick: (PolicyItem) -> Unit
) : RecyclerView.Adapter<PolicyAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemPolicyRowBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            // '자세히 보기' 버튼 클릭 리스너 설정
            binding.btnPolDetail.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDetailClick(policies[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // ItemPolicyRowBinding은 item_policy_row.xml 파일명에 따라 자동 생성됩니다.
        val binding = ItemPolicyRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = policies[position]

        with(holder.binding) {
            // 1. 사업명  
            tvPolTitle.text = item.TITLE ?: "제목 없음"

            // 2. 주요 목적 및 내용  
            tvPolDescrpt.text = item.MAIN_PURPS ?: "내용 정보가 없습니다."

            // 3. 운영주체 및 조직 
            val mainBody = item.OPERT_MAINBD_NM ?: ""
            val orgName = item.OPERT_ORGNZT_NM ?: ""
            // 둘 다 비어있으면 "-" 표시, 또는 공백으로 구분하여 나열
            val operationInfo = if (mainBody.isBlank() && orgName.isBlank()) "-" else "$mainBody $orgName".trim()
            tvPolOprt.text = "운영주체: $operationInfo"

            // 4. 문의처  
            tvPolContact.text = "문의처: ${item.GUID ?: "-"}"
        }
    }

    override fun getItemCount() = policies.size

    fun updateData(newPolicies: List<PolicyItem>) {
        policies = newPolicies
        notifyDataSetChanged()
    }

}
