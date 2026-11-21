export type Session = { token: string; role: 'ADMIN' | 'TEACHER'; name: string }
export type Group = { id: number; name: string; course: string; students: number }
export type Student = { id: number; fullName: string; email: string; groupId: number }
export type Lesson = { id: number; groupId: number; subject: string; room: string; weekday: number; startHour: number; endHour: number }
export type Page<T> = { items: T[]; page: number; pages: number; total: number }

let token = ''
export const setToken = (t: string) => { token = t }

export async function api<T>(path: string, method = 'GET', body?: unknown): Promise<T> {
  const r = await fetch('/api' + path, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  })
  if (!r.ok) {
    const m = await r.json().catch(() => null)
    throw new Error(m?.message ?? `Request failed (${r.status})`)
  }
  const text = await r.text()
  return (text ? JSON.parse(text) : undefined) as T
}
