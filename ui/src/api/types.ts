export interface Info {
  name: string;
  version: string;
  operationsEnabled: boolean;
  demo: boolean;
}
export interface Job {
  name: string;
  registered: boolean;
}
export interface Step {
  id: string;
  name: string;
  status: string;
  readCount: number;
  writeCount: number;
  skipCount: number;
  commitCount: number;
  rollbackCount: number;
  startTime: string | null;
  endTime: string | null;
  durationMillis: number;
  error: string;
}
export interface Execution {
  id: string;
  instanceId: string;
  jobName: string;
  status: string;
  createTime: string;
  startTime: string | null;
  endTime: string | null;
  durationMillis: number;
  readCount: number;
  writeCount: number;
  skipCount: number;
  exitCode: string;
  error: string;
  parameters: Record<string, string>;
  steps: Step[];
}
