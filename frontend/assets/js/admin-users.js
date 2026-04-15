document.addEventListener('DOMContentLoaded', () => {
    // Show lock date only if temporary
    const lockDuration = document.getElementById('lock-duration');
    const lockDateGroup = document.getElementById('lock-date-group');
    if (lockDuration) {
        lockDuration.addEventListener('change', () => {
            lockDateGroup.style.display = lockDuration.value === 'permanent' ? 'none' : 'block';
        });
    }

    let targetLockButton = null;
    let targetUserName = '';

    // Handle Lock and Unlock Clicks using Event Delegation
    document.body.addEventListener('click', (e) => {
        // Unlock Logic
        const unlockBtn = e.target.closest('button');
        if (unlockBtn && unlockBtn.textContent.includes('🔓 Mở khóa')) {
            e.preventDefault();
            if (confirm('Bạn có chắc chắn muốn mở khóa tài khoản này?')) {
                // Change UI of the button to Lock again
                unlockBtn.innerHTML = '🔒';
                unlockBtn.className = 'btn btn--danger btn--sm btn-delete';
                unlockBtn.style = ''; // remove inline styles
                
                const tr = unlockBtn.closest('tr');
                if (tr) {
                    const statusDot = tr.querySelector('.status-dot');
                    if (statusDot) {
                        statusDot.className = 'status-dot status-dot--active';
                        statusDot.textContent = 'Hoạt động';
                    }
                }
                if (typeof showToast === 'function') {
                    showToast('Đã mở khóa tài khoản thành công.', 'success');
                }
            }
            return;
        }

        // Lock Logic
        const lockBtn = e.target.closest('button');
        if (lockBtn && lockBtn.textContent.includes('🔒')) {
            e.preventDefault();
            targetLockButton = lockBtn;
            targetUserName = lockBtn.getAttribute('data-name') || 'Tài khoản người dùng';
            
            const modal = document.getElementById('lock-account-modal');
            const nameSpan = document.getElementById('lock-user-name');
            if (modal && nameSpan) {
                nameSpan.textContent = targetUserName;
                document.getElementById('lock-duration').value = 'temporary';
                lockDateGroup.style.display = 'block';
                document.getElementById('lock-date').value = '';
                modal.classList.add('active');
            }
            return;
        }
    });

    // Confirm Lock Action
    const confirmLockBtn = document.getElementById('btn-confirm-lock');
    if (confirmLockBtn) {
        confirmLockBtn.addEventListener('click', () => {
            const isTemp = document.getElementById('lock-duration').value === 'temporary';
            const dateVal = document.getElementById('lock-date').value;
            
            if (isTemp && !dateVal) {
                if (typeof showToast === 'function') showToast('Vui lòng chọn ngày khóa!', 'error');
                else alert('Vui lòng chọn ngày khóa!');
                return;
            }

            const tr = targetLockButton.closest('tr');
            if (tr) {
                const statusDot = tr.querySelector('.status-dot');
                if (statusDot) {
                    statusDot.className = 'status-dot status-dot--inactive';
                    statusDot.textContent = 'Bị khóa';
                }
                
                // Change UI to Unlock button
                targetLockButton.innerHTML = '🔓 Mở khóa';
                targetLockButton.className = 'btn btn--sm';
                targetLockButton.style.background = 'var(--color-success)';
                targetLockButton.style.color = '#fff';
                targetLockButton.style.border = 'none';
                targetLockButton.style.borderRadius = '6px';
                targetLockButton.style.padding = '6px 12px';
            }

            const modal = document.getElementById('lock-account-modal');
            if (modal) modal.classList.remove('active');

            if (typeof showToast === 'function') {
                const msg = isTemp ? `Đã khóa tài khoản đến ngày ${dateVal.split('-').reverse().join('/')}` : 'Đã khóa tài khoản vĩnh viễn';
                showToast(msg, 'success');
            }
        });
    }
});